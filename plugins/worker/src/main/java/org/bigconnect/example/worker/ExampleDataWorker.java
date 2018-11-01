package org.bigconnect.example.worker;

import com.mware.core.exception.BcException;
import com.mware.core.ingest.dataworker.DataWorker;
import com.mware.core.ingest.dataworker.DataWorkerData;
import com.mware.core.model.clientapi.dto.ClientApiWorkspace;
import com.mware.core.model.clientapi.dto.VisibilityJson;
import com.mware.ge.*;
import com.mware.ge.property.StreamingPropertyValue;
import com.mware.core.model.Description;
import com.mware.core.model.Name;
import com.mware.core.model.graph.GraphUpdateContext;
import com.mware.core.model.properties.types.PropertyMetadata;
import com.mware.core.model.workQueue.Priority;
import com.mware.core.model.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mware.core.model.properties.BcProperties.CONCEPT_TYPE;
import static com.mware.core.model.properties.BcProperties.MIME_TYPE;
import static com.mware.core.model.properties.BcProperties.RAW;

@Name("Example Data Worker")
@Description("Creates person entities from an imported CSV file.")
public class ExampleDataWorker extends DataWorker {

    @Override
    public boolean isHandled(Element element, Property property) {
        // This worker is only interested in vertices with a RAW property whose content is a CSV file.
        return element instanceof Vertex &&
                property != null &&
                CONCEPT_TYPE.getProperty(element) == null &&
                RAW.getPropertyName().equals(property.getName()) &&
                "text/csv".equals(MIME_TYPE.getOnlyPropertyValue(element));
    }

    @Override
    public void execute(InputStream inputStream, DataWorkerData workData) throws Exception {
        // The visibility provided by workData will be used on new vertices, edges, and properties.
        // This visibility originates from user input when uploading the CSV file.
        Visibility visibility = workData.getVisibility();
        VisibilityJson visibilityJson = workData.getElementVisibilityJson();

        // This is the vertex containing the CSV file content on its RAW property. RAW is a streaming property, which
        // is used for very large values. In this case, it's convenient to copy the value content to a temporary file.
        Vertex fileVertex = (Vertex) workData.getElement();
        StreamingPropertyValue raw = RAW.getPropertyValue(fileVertex);
        File file = copyToTempFile(raw);

        Set<Element> newElements = new HashSet<>();
        try (GraphUpdateContext ctx = getGraphRepository().beginGraphUpdate(Priority.HIGH, getUser(), getAuthorizations())) {
            if (Contact.containsContacts(file)) {
                ctx.update(fileVertex, elemCtx -> {
                    elemCtx.setConceptType(OntologyConstants.CONTACTS_CSV_FILE_CONCEPT_TYPE);
                });

                Stream<Contact> contacts = Contact.readContacts(file);
                contacts.forEach(contact -> {
                    try {
                        // Create a new vertex representing the person entity and create an edge representing the
                        // relationship between the CSV file and the person.
                        Vertex personVertex = createPersonVertex(ctx, contact, visibilityJson, visibility).get();
                        Edge edge = createFileToPersonEdge(ctx, fileVertex, personVertex, visibilityJson, visibility).get();
                        newElements.add(personVertex);
                        if (edge != null) {
                            newElements.add(edge);
                        }
                    } catch (Exception ex) {
                        throw new BcException("Could not import contact", ex);
                    }
                });
            }
        } finally {
            file.delete();
        }

        // Get the workspace that the CSV file vertex was uploaded to.
        Workspace workspace = getWorkspaceRepository().findById(workData.getWorkspaceId(), getUser());

        // Add all new vertices to the workspace. Edges are automatically brought into the workspace.
        addVerticesToWorkspace(newElements, workspace);

        // Notify all browser clients so viewers of the workspace see the new elements immediately.
        notifyUserInterfaceClients(workspace);
    }

    private void addVerticesToWorkspace(Set<Element> newElements, Workspace workspace) {
        Collection<String> vertexIds =
                newElements.stream()
                        .filter(element -> element instanceof Vertex)
                        .map(Element::getId)
                        .collect(Collectors.toList());
        getWorkspaceRepository().updateEntitiesOnWorkspace(workspace, vertexIds, getUser());
        getGraph().flush();
    }

    private void notifyUserInterfaceClients(Workspace workspace) {
        ClientApiWorkspace apiWorkspace = getWorkspaceRepository().toClientApi(workspace, getUser(), getAuthorizations());
        getWorkQueueRepository().pushWorkspaceChange(apiWorkspace, Collections.emptyList(), getUser().getUserId(), null);
    }

    private GraphUpdateContext.UpdateFuture<Vertex> createPersonVertex(
            GraphUpdateContext ctx,
            Contact contact,
            VisibilityJson visibilityJson,
            Visibility visibility
    ) {
        return ctx.getOrCreateVertexAndUpdate("PERSON_" + UUID.randomUUID(), visibility, elemCtx -> {
            elemCtx.setConceptType(OntologyConstants.PERSON_CONCEPT_TYPE);
            elemCtx.updateBuiltInProperties(propertyMetadata(visibilityJson));
            OntologyConstants.PERSON_FULL_NAME_PROPERTY.updateProperty(elemCtx, contact.name, propertyMetadata(visibilityJson));
            OntologyConstants.PERSON_PHONE_NUMBER_PROPERTY.updateProperty(elemCtx, contact.phone, propertyMetadata(visibilityJson));
            OntologyConstants.PERSON_EMAIL_ADDRESS_PROPERTY.updateProperty(elemCtx, contact.email, propertyMetadata(visibilityJson));
        });
    }

    private GraphUpdateContext.UpdateFuture<Edge> createFileToPersonEdge(
            GraphUpdateContext ctx,
            Vertex fileVertex,
            Vertex personVertex,
            VisibilityJson visibilityJson,
            Visibility visibility
    ) {
        String edgeId = "HAS_ENTITY_" + UUID.randomUUID();
        String fromVertexId = fileVertex.getId();
        String toVertexId = personVertex.getId();
        if (!getGraph().doesEdgeExist(edgeId, getAuthorizations())){
            EdgeBuilderByVertexId e = getGraph().prepareEdge(edgeId, fromVertexId, toVertexId, OntologyConstants.HAS_ENTITY_EDGE_LABEL, visibility);
            return ctx.update(e, elemCtx -> {
                elemCtx.updateBuiltInProperties(propertyMetadata(visibilityJson));
            });
        }
        return null;
    }

    private PropertyMetadata propertyMetadata(VisibilityJson visibilityJson) {
        Visibility defaultVisibility = getVisibilityTranslator().getDefaultVisibility();
        return new PropertyMetadata(getUser(), visibilityJson, defaultVisibility);
    }

    private File copyToTempFile(StreamingPropertyValue spv) throws IOException {
        Path tempPath = Files.createTempFile(getClass().getName(), ".csv");
        try (InputStream inputStream = spv.getInputStream()) {
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempPath.toFile();
    }
}
