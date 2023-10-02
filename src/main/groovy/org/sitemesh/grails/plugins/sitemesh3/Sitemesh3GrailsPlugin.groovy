package org.sitemesh.grails.plugins.sitemesh3

import grails.plugins.Plugin
import org.grails.config.PropertySourcesConfig
import org.grails.web.sitemesh.GroovyPageLayoutFinder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource

class Sitemesh3GrailsPlugin extends Plugin {

    def grailsVersion = "6.0.0  > *"
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "SiteMesh 3"
    def author = "Scott Murphy"
    def authorEmail = ""
    def description = "Configures Grails to use SiteMesh 3 instead of SiteMesh 2"
    def profiles = ['web']
    def documentation = "https://github.com/codeconsole/grails-sitemesh3"

    def license = "APACHE"
    def organization = [name: "SiteMesh", url: "https://github.com/sitemesh"]
    def developers = [[name: "Scott Murphy"]]
    def issueManagement = [system: "GitHub", url: "https://github.com/codeconsole/grails-sitemesh3/issues"]
    def scm = [url: "https://github.com/codeconsole/grails-sitemesh3"]

    def loadBefore = ['groovyPages']

    static PropertySource getDefaultPropertySource() {
        return new MapPropertySource("sitemesh3Properties", [
                'grails.gsp.view.layoutViewResolver': 'false',
                'sitemesh.decorator.default': 'main',
                'sitemesh.decorator.metaTag': 'layout',
                'sitemesh.decorator.attribute': GroovyPageLayoutFinder.LAYOUT_ATTRIBUTE,
                'sitemesh.decorator.prefix': '/layouts/',
                'sitemesh.decorator.bundles': ['sm2'],
                'grails.views.gsp.sitemesh.preprocess': 'false'
        ])
    }

    Closure doWithSpring() {
        { ->
            def propertySources = application.mainContext.environment.getPropertySources()
            propertySources.addFirst(getDefaultPropertySource())
            application.config = new PropertySourcesConfig(propertySources)
            grailsLayoutHandlerMapping(GrailsLayoutHandlerMapping)
        }
    }

    void doWithApplicationContext() {}

    void doWithDynamicMethods() {}

    void onChange(Map<String, Object> event) {}

    void onConfigChange(Map<String, Object> event) {}

    void onShutdown(Map<String, Object> event) {}
}