define(['public/v1/api'], function(bc) {
    'use strict';

    bc.registry.registerExtension('org.bigconnect.authentication', {
        componentPath: 'org/bigconnect/example/auth/authentication'
    })
});
