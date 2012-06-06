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
package com.tyndalehouse.step.core.utils;

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.crosswire.common.util.Language;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.FeatureType;

import com.tyndalehouse.step.core.models.BibleVersion;

/**
 * a set of utility methods to manipulate the JSword objects coming out
 * 
 * @author chrisburrell
 * 
 */
public final class JSwordUtils {
    /**
     * hiding implementaiton
     */
    private JSwordUtils() {
        // no implementation
    }

    /**
     * returns a sorted list from another list, with only the required information
     * 
     * @param bibles a list of jsword bibles
     * @return the list of bibles
     */
    public static List<BibleVersion> getSortedSerialisableList(final Collection<Book> bibles) {
        final List<BibleVersion> versions = new ArrayList<BibleVersion>();

        // we only send back what we need
        for (final Book b : bibles) {
            final BibleVersion v = new BibleVersion();
            v.setName(b.getName());
            v.setInitials(b.getInitials());

            final Language language = b.getLanguage();
            if (language != null) {

                v.setLanguage(language.getName());
            }
            v.setHasStrongs(b.hasFeature(FeatureType.STRONGS_NUMBERS));
            v.setHasMorphology(b.hasFeature(FeatureType.MORPHOLOGY));

            versions.add(v);
        }

        // finally sort by initials
        sort(versions, new Comparator<BibleVersion>() {
            @Override
            public int compare(final BibleVersion o1, final BibleVersion o2) {
                return o1.getInitials().compareTo(o2.getInitials());
            }
        });

        return versions;
    }
}