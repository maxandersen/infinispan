/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.modules.ModuleLoader;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigurationService extends AbstractCacheConfigurationService {

    interface Dependencies {
        ModuleLoader getModuleLoader();
        EmbeddedCacheManager getCacheContainer();
        TransactionManager getTransactionManager();
        TransactionSynchronizationRegistry getTransactionSynchronizationRegistry();
        Configuration getTemplateConfiguration();
    }

    private final ConfigurationBuilder builder;
    private final Dependencies dependencies;

    public CacheConfigurationService(String name, ConfigurationBuilder builder, Dependencies dependencies) {
        super(name);
        this.builder = builder;
        this.dependencies = dependencies;
    }

    @Override
    protected EmbeddedCacheManager getCacheContainer() {
        return this.dependencies.getCacheContainer();
    }

    @Override
    protected ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        Configuration templateConfiguration = this.dependencies.getTemplateConfiguration();
        if (templateConfiguration != null) {
            builder.read(templateConfiguration);
        }
        Configuration configuration = this.builder.build();
        builder.read(configuration);
        builder.template(configuration.isTemplate());

        GlobalConfiguration globalConfiguration = SecurityActions.getCacheManagerConfiguration(dependencies.getCacheContainer());
        builder.statistics().enabled(globalConfiguration.statistics());
        TransactionManager tm = this.dependencies.getTransactionManager();
        if (tm != null) {
            builder.transaction().transactionManagerLookup(new TransactionManagerProvider());
        }
        TransactionSynchronizationRegistry tsr = this.dependencies.getTransactionSynchronizationRegistry();
        if (tsr != null) {
            builder.transaction().transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr));
        }
        return builder;
    }
}
