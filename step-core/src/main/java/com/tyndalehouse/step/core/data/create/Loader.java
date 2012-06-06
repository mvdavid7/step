/*******************************************************************************
 * Copyright (c) 2012, Directors of the Tyndale STEP Project
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright 
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright 
 * notice, this list of conditions and the following disclaimer in 
 * the documentation and/or other materials provided with the 
 * distribution.
 * Neither the name of the Tyndale House, Cambridge (www.TyndaleHouse.com)  
 * nor the names of its contributors may be used to endorse or promote 
 * products derived from this software without specific prior written 
 * permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS 
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT 
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING 
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
 * THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.tyndalehouse.step.core.data.create;

import java.util.Properties;

import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.google.inject.Inject;
import com.tyndalehouse.step.core.data.create.loaders.CsvModuleLoader;
import com.tyndalehouse.step.core.data.create.loaders.CustomTranslationCsvModuleLoader;
import com.tyndalehouse.step.core.data.create.loaders.translations.OpenBibleDataTranslation;
import com.tyndalehouse.step.core.data.create.loaders.translations.TimelineEventTranslation;
import com.tyndalehouse.step.core.data.entities.GeoPlace;
import com.tyndalehouse.step.core.data.entities.HotSpot;
import com.tyndalehouse.step.core.data.entities.TimelineEvent;
import com.tyndalehouse.step.core.data.entities.morphology.Morphology;
import com.tyndalehouse.step.core.exceptions.StepInternalException;
import com.tyndalehouse.step.core.service.JSwordService;

/**
 * The object that will be responsible for loading all the data into a database
 * 
 * @author chrisburrell
 * 
 */
public class Loader {
    private static final int INSTALL_WAITING = 1000;
    private static final int INSTALL_MAX_WAITING = INSTALL_WAITING * 180;
    private static final String KJV = "KJV";
    private static final Logger LOG = LoggerFactory.getLogger(Loader.class);
    private final EbeanServer ebean;
    private final JSwordService jsword;
    private final Properties coreProperties;

    /**
     * The loader is given a connection source to load the data
     * 
     * @param jsword the jsword service
     * @param ebean the persistence server
     * @param coreProperties the step core properties
     */
    @Inject
    public Loader(final JSwordService jsword, final EbeanServer ebean,
            @Named("StepCoreProperties") final Properties coreProperties) {
        this.jsword = jsword;
        this.ebean = ebean;
        this.coreProperties = coreProperties;
    }

    /**
     * Creates the table and loads the initial data set
     */
    public void init() {
        // in order to do this, we need some jsword modules available. - we assume someone has kicked off the
        // process
        // kick of installation of jsword modules
        checkAndWaitForKJV();

        // now we can load the data
        loadData();
    }

    /**
     * All modules are based on this version
     */
    private void checkAndWaitForKJV() {
        int waitTime = INSTALL_MAX_WAITING;

        // very ugly, but as good as it's going to get for now
        while (waitTime > 0 && !this.jsword.isInstalled(KJV)) {
            try {
                LOG.debug("Waiting for KJV installation to finish...");
                waitTime -= INSTALL_WAITING;
                Thread.sleep(INSTALL_WAITING);
            } catch (final InterruptedException e) {
                LOG.warn("Interrupted exception", e);
            }
        }

        if (waitTime <= 0) {
            throw new StepInternalException("KJV module was not installed in time");
        }
    }

    /**
     * Loads the data into the database
     */
    private void loadData() {
        LOG.debug("Loading initial data");
        this.ebean.beginTransaction();

        try {
            loadHotSpots();
            loadTimeline();
            loadOpenBibleGeography();
            loadDictionaryArticles();
            loadRobinsonMorphology();
            this.ebean.commitTransaction();
        } finally {
            this.ebean.endTransaction();
        }
    }

    /**
     * loads all hotspots
     * 
     * @return number of records loaded
     */
    int loadHotSpots() {
        return new CsvModuleLoader<HotSpot>(this.ebean,
                this.coreProperties.getProperty("test.data.path.timeline.hotspots"), HotSpot.class).init();
    }

    /**
     * Loads all of robinson's morphological data
     * 
     * @return the number of entries
     */
    int loadRobinsonMorphology() {
        return new CsvModuleLoader<Morphology>(this.ebean,
                this.coreProperties.getProperty("test.data.path.morphology.robinson"), Morphology.class)
                .init();
    }

    /**
     * loads the timeline events
     * 
     * @return number of records loaded
     */
    int loadTimeline() {
        return new CustomTranslationCsvModuleLoader<TimelineEvent>(this.ebean,
                this.coreProperties.getProperty("test.data.path.timeline.events.directory"),
                TimelineEvent.class, new TimelineEventTranslation(this.jsword)).init();
    }

    /**
     * loads the open bible geography data
     * 
     * @return the number of records loaded
     */
    int loadOpenBibleGeography() {
        return new CustomTranslationCsvModuleLoader<GeoPlace>(this.ebean,
                this.coreProperties.getProperty("test.data.path.geography.openbible"), GeoPlace.class,
                new OpenBibleDataTranslation(this.jsword), '\t').init();
    }

    /**
     * loads a set of articles
     * 
     * @return the number of articles loaded
     */
    int loadDictionaryArticles() {
        return new DictionaryLoader(this.ebean, this.jsword,
                this.coreProperties.getProperty("test.data.path.dictionary.easton")).init();
    }

}