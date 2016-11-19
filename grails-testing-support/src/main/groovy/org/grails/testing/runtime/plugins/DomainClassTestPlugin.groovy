/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.grails.testing.runtime.plugins

import grails.core.GrailsApplication
import grails.validation.ConstrainedProperty
import grails.validation.ConstraintsEvaluator
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.datastore.gorm.events.AutoTimestampEventListener
import org.grails.datastore.gorm.events.DomainEventListener
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.model.AbstractMappingContext
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.grails.plugins.domain.DomainClassGrailsPlugin
import org.grails.plugins.domain.support.GrailsDomainClassCleaner
import org.grails.testing.runtime.TestEvent
import org.grails.testing.runtime.TestPlugin
import org.grails.testing.runtime.TestRuntime
import org.grails.validation.ConstraintsEvaluatorFactoryBean
import org.springframework.context.ConfigurableApplicationContext

/**
 * a TestPlugin for TestRuntime for adding Grails DomainClass (GORM) support
 * - this implementation uses SimpleMapDatastore GORM implementation
 * 
 *
 */
@CompileStatic
class DomainClassTestPlugin implements TestPlugin {
    String[] requiredFeatures = ['grailsApplication', 'coreBeans']
    String[] providedFeatures = ['domainClass']
    int ordinal = 0

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void registerBeans(TestRuntime runtime, GrailsApplication grailsApplication) {
        defineBeans(runtime) {
            grailsDatastore(SimpleMapDatastore, grailsApplication.mainContext)
            transactionManager(DatastoreTransactionManager) {
                datastore = ref("grailsDatastore")
            }
            "${ConstraintsEvaluator.BEAN_NAME}"(ConstraintsEvaluatorFactoryBean) {
                defaultConstraints = DomainClassGrailsPlugin.getDefaultConstraints(grailsApplication.config)
            }
            grailsDomainClassCleaner(GrailsDomainClassCleaner, grailsApplication)
        }
    }
    
    protected void applicationInitialized(TestRuntime runtime, GrailsApplication grailsApplication) {
        initializeDatastoreImplementation(grailsApplication)
    }
    
    protected void initializeDatastoreImplementation(GrailsApplication grailsApplication) {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)grailsApplication.mainContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        ((AbstractMappingContext)simpleDatastore.mappingContext).setCanInitializeEntities(false)
        applicationContext.addApplicationListener applicationContext.getBean(GrailsDomainClassCleaner)
        applicationContext.addApplicationListener new DomainEventListener(simpleDatastore)
        applicationContext.addApplicationListener new AutoTimestampEventListener(simpleDatastore)
    }

    protected void cleanupDatastore() {
        ClassPropertyFetcher.clearCache()
        ConstrainedProperty.removeConstraint("unique")
    }

    protected void connectDatastore(TestRuntime runtime, GrailsApplication grailsApplication) {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)grailsApplication.mainContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(simpleDatastore))
        Session currentSession = DatastoreUtils.bindSession(simpleDatastore.connect())
        runtime.putValue("domainClassCurrentSession", currentSession)
    }

    protected void shutdownDatastoreImplementation(TestRuntime runtime, GrailsApplication grailsApplication) {
        Session currentSession = (Session)runtime.removeValue("domainClassCurrentSession")
        if (currentSession != null) {
            currentSession.disconnect()
            DatastoreUtils.unbindSession(currentSession)
        }
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext)grailsApplication.mainContext
        SimpleMapDatastore simpleDatastore = applicationContext.getBean(SimpleMapDatastore)
        simpleDatastore.clearData()
    }

    protected void before(TestRuntime runtime, GrailsApplication grailsApplication) {
        connectDatastore(runtime, grailsApplication)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    protected void after(TestRuntime runtime, GrailsApplication grailsApplication) {
        shutdownDatastoreImplementation(runtime, grailsApplication)
    }
    
    void defineBeans(TestRuntime runtime, Closure closure) {
        runtime.publishEvent("defineBeans", [closure: closure])
    }
    
    GrailsApplication getGrailsApplication(TestEvent event) {
        (GrailsApplication)event.runtime.getValue("grailsApplication")
    }

    public void onTestEvent(TestEvent event) {
        switch(event.name) {
            case 'before':
                before(event.runtime, getGrailsApplication(event))
                break
            case 'after':
                after(event.runtime, getGrailsApplication(event))
                break
            case 'registerBeans':
                registerBeans(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'applicationInitialized':
                applicationInitialized(event.runtime, (GrailsApplication)event.arguments.grailsApplication)
                break
            case 'beforeClass':
                ClassPropertyFetcher.clearCache()
                break
            case 'afterClass':
                cleanupDatastore()
                break
        }
    }
    
    public void close(TestRuntime runtime) {
        
    }
}
