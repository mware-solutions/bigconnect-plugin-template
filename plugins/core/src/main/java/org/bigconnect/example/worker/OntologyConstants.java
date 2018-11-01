package org.bigconnect.example.worker;

import com.mware.core.model.properties.types.StringSingleValueBcProperty;

public interface OntologyConstants {
    String ONTOLOGY_BASE_IRI = "test#";

    String PERSON_CONCEPT_TYPE = ONTOLOGY_BASE_IRI + "person";

    String CONTACTS_CSV_FILE_CONCEPT_TYPE = ONTOLOGY_BASE_IRI + "contactsCsvFile";

    String HAS_ENTITY_EDGE_LABEL = ONTOLOGY_BASE_IRI + "hasEntity";
    String KNOWS_EDGE_LABEL = ONTOLOGY_BASE_IRI + "knows";

    StringSingleValueBcProperty PERSON_FULL_NAME_PROPERTY =
            new StringSingleValueBcProperty(ONTOLOGY_BASE_IRI + "fullName");

    StringSingleValueBcProperty PERSON_PHONE_NUMBER_PROPERTY =
            new StringSingleValueBcProperty(ONTOLOGY_BASE_IRI + "phoneNumber");

    StringSingleValueBcProperty PERSON_EMAIL_ADDRESS_PROPERTY =
            new StringSingleValueBcProperty(ONTOLOGY_BASE_IRI + "emailAddress");
}
