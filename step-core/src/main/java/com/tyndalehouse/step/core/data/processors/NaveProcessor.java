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
package com.tyndalehouse.step.core.data.processors;

import javax.inject.Inject;

import org.apache.lucene.document.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tyndalehouse.step.core.data.EntityConfiguration;
import com.tyndalehouse.step.core.data.create.PostProcessor;
import com.tyndalehouse.step.core.exceptions.StepInternalException;
import com.tyndalehouse.step.core.service.jsword.JSwordPassageService;

/**
 * Adds generated fields to the entity document - affects both "definition" and "specificForm"
 * 
 * @author chrisburrell
 * 
 */
public class NaveProcessor implements PostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NaveProcessor.class);
    private final JSwordPassageService jswordPassage;

    /**
     * Instantiates a new reference processor.
     * 
     * @param jswordPassage the jsword passage
     */
    @Inject
    public NaveProcessor(final JSwordPassageService jswordPassage) {
        this.jswordPassage = jswordPassage;
    }

    @Override
    public void process(final EntityConfiguration config, final Document doc) {
        doc.add(config.getField("rootStem", doc.get("root")));
        doc.add(config.getField("expandedReferences", expandRefs(doc.get("references"))));

    }

    /**
     * Expand refs to their full blown set.
     * 
     * @param refs the string
     * @return the string
     */
    private String expandRefs(final String refs) {
        try {
            return this.jswordPassage.getAllReferences(refs, "ESV");
        } catch (final StepInternalException ex) {
            LOGGER.error("Nave data: {}", ex.getMessage());
            LOGGER.trace("Expanded refs failed", ex);
        }
        return refs;
    }
}
