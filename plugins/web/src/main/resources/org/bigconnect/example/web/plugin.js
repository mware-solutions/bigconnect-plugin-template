require(['public/v1/api'], function(bc) {
    'use strict';

    bc.registry.registerExtension('org.bigconnect.detail.toolbar', {
        title: i18n('org.bigconnect.example.web.detail.toolbar.google'),
        event: 'google',
        canHandle: function(objects) {
            return objects.vertices.length === 1 && objects.edges.length === 0
                && objects.vertices[0].conceptType === 'test#person';
        }
    });

    bc.connect().then(function(api) {
        $(document).on('google', function(e, data) {
            var person = data.vertices[0];
            var name = api.formatters.vertex.prop(person, 'test#fullName');
            var url = 'http://www.google.com/#safe=on&q=' + name;
            window.open(url, '_blank');
        });
    });
});
