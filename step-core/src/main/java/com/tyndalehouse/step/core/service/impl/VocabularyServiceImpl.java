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
package com.tyndalehouse.step.core.service.impl;

import static com.tyndalehouse.step.core.utils.StringUtils.isBlank;
import static com.tyndalehouse.step.core.utils.StringUtils.split;
import static com.tyndalehouse.step.core.utils.ValidateUtils.notBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.tyndalehouse.step.core.utils.SortingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tyndalehouse.step.core.data.EntityDoc;
import com.tyndalehouse.step.core.data.EntityIndexReader;
import com.tyndalehouse.step.core.data.EntityManager;
import com.tyndalehouse.step.core.exceptions.UserExceptionType;
import com.tyndalehouse.step.core.models.LexiconSuggestion;
import com.tyndalehouse.step.core.models.VocabResponse;
import com.tyndalehouse.step.core.service.VocabularyService;
import com.tyndalehouse.step.core.service.helpers.OriginalWordUtils;

/**
 * defines all vocab related queries
 *
 * @author chrisburrell
 *
 */
@Singleton
public class VocabularyServiceImpl implements VocabularyService {
    private static final String MULTI_WORD_SEPARATOR = " | ";
    private static final Logger LOGGER = LoggerFactory.getLogger(VocabularyServiceImpl.class);
    private static final String STRONG_SEPARATORS = "[ ,]+";
    private static final String HIGHER_STRONG = "STRONG:";
    private static final String LOWER_STRONG = "strong:";
    private static final int START_STRONG_KEY = HIGHER_STRONG.length();
    private final EntityIndexReader definitions;

    // define a few extraction methods
    private final LexiconDataProvider transliterationProvider = new LexiconDataProvider() {
        @Override
        public String getData(final EntityDoc l) {
            return l.get("stepTransliteration");
        }
    };
    private final LexiconDataProvider englishVocabProvider = new LexiconDataProvider() {
        @Override
        public String getData(final EntityDoc l) {
            return l.get("stepGloss");
        }
    };
    private final LexiconDataProvider greekVocabProvider = new LexiconDataProvider() {
        @Override
        public String getData(final EntityDoc l) {
            return l.get("accentedUnicode");
        }
    };

    /**
     * @param manager the entity manager
     */
    @Inject
    public VocabularyServiceImpl(final EntityManager manager) {
        this.definitions = manager.getReader("definition");
    }

    @Override
    public VocabResponse getDefinitions(final String vocabIdentifiers) {
        notBlank(vocabIdentifiers, "Vocab identifiers was null", UserExceptionType.SERVICE_VALIDATION_ERROR);
        final String[] strongList = getKeys(vocabIdentifiers);

        if (strongList.length != 0) {
            final EntityDoc[] strongDefs = this.definitions.searchUniqueBySingleField("strongNumber",
                    strongList);

            final EntityDoc[] definitions = reOrder(strongList, strongDefs);
            final Map<String, List<LexiconSuggestion>> relatedWords = readRelatedWords(definitions);
            return new VocabResponse(definitions, relatedWords);
        }

        return new VocabResponse();
    }

    /**
     * Read related words, i.e. all the words that are in the related numbers fields.
     *
     * @param defs the definitions that have been looked up.
     * @return the map
     */
    private Map<String, List<LexiconSuggestion>> readRelatedWords(final EntityDoc[] defs) {
        // this map keys the original word strong number to all the related codes
        final Map<String, SortedSet<LexiconSuggestion>> relatedWords = new HashMap<String, SortedSet<LexiconSuggestion>>(
                defs.length * 2);

        // to avoid doing lookups twice, we key each short definition by its code as well
        final Map<String, LexiconSuggestion> lookedUpWords = new HashMap<String, LexiconSuggestion>(
                defs.length * 2);

        for (final EntityDoc doc : defs) {
            final String sourceNumber = doc.get("strongNumber");
            final String relatedWordNumbers = doc.get("relatedNumbers");
            final String[] allRelatedWords = split(relatedWordNumbers, "[ ,]+");
            for (final String relatedWord : allRelatedWords) {
                LexiconSuggestion shortLexiconDefinition = lookedUpWords.get(relatedWord);

                // look up related word from index
                if (shortLexiconDefinition == null) {
                    final EntityDoc[] relatedDoc = this.definitions.searchUniqueBySingleField("strongNumber",
                            relatedWord);
                    // assume first doc
                    if (relatedDoc.length > 0) {
                        shortLexiconDefinition = OriginalWordUtils.convertToSuggestion(relatedDoc[0]);
                        lookedUpWords.put(relatedWord, shortLexiconDefinition);
                    }
                }

                // store as a link to its source number
                if (shortLexiconDefinition != null) {
                    SortedSet<LexiconSuggestion> associatedNumbersSoFar = relatedWords.get(sourceNumber);
                    if (associatedNumbersSoFar == null) {
                        associatedNumbersSoFar = new TreeSet<LexiconSuggestion>(
                                SortingUtils.LEXICON_SUGGESTION_COMPARATOR);
                        relatedWords.put(sourceNumber, associatedNumbersSoFar);
                    }

                    associatedNumbersSoFar.add(shortLexiconDefinition);
                }
            }
        }
        return convertToListMap(relatedWords);
    }

    /**
     * Convert to list map, from a map of sets to a map of lists. This also orders the definitions.
     *
     * @param relatedWords the related words
     * @return the map
     */
    private Map<String, List<LexiconSuggestion>> convertToListMap(
            final Map<String, SortedSet<LexiconSuggestion>> relatedWords) {
        final Map<String, List<LexiconSuggestion>> results = new HashMap<String, List<LexiconSuggestion>>();
        for (final Entry<String, SortedSet<LexiconSuggestion>> relatedWordSet : relatedWords.entrySet()) {
            results.put(relatedWordSet.getKey(), new ArrayList<LexiconSuggestion>(relatedWordSet.getValue()));
        }
        return results;
    }

    /**
     * Re-orders based on the input.
     *
     * @param strongList the order list of stongs
     * @param strongDefs the definitions that have been found
     * @return the entity doc[]
     */
    private EntityDoc[] reOrder(final String[] strongList, final EntityDoc[] strongDefs) {
        final Map<String, EntityDoc> entitiesByStrong = new HashMap<String, EntityDoc>(strongList.length * 2);
        for (final EntityDoc def : strongDefs) {
            entitiesByStrong.put(def.get("strongNumber"), def);
        }

        final EntityDoc[] results = new EntityDoc[strongDefs.length];
        int current = 0;
        for (final String strong : strongList) {
            final EntityDoc entityDoc = entitiesByStrong.get(strong);
            if (entityDoc != null) {
                results[current++] = entityDoc;
            }
        }

        return results;
    }

    @Override
    public VocabResponse getQuickDefinitions(final String vocabIdentifiers) {
        notBlank(vocabIdentifiers, "Vocab identifiers was null", UserExceptionType.SERVICE_VALIDATION_ERROR);
        final String[] strongList = getKeys(vocabIdentifiers);

        if (strongList.length != 0) {
            return new VocabResponse(this.definitions.searchUniqueBySingleField("strongNumber", strongList));
        }
        return new VocabResponse();
    }

    @Override
    public String getEnglishVocab(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.englishVocabProvider);
    }

    @Override
    public String getGreekVocab(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.greekVocabProvider);
    }

    @Override
    public String getDefaultTransliteration(final String vocabIdentifiers) {
        return getDataFromLexiconDefinition(vocabIdentifiers, this.transliterationProvider);
    }

    /**
     * gets data from the matched lexicon definitions
     *
     * @param vocabIdentifiers the identifiers
     * @param provider the provider used to get data from it
     * @return the data in String form
     */
    private String getDataFromLexiconDefinition(final String vocabIdentifiers,
            final LexiconDataProvider provider) {
        final String[] keys = getKeys(vocabIdentifiers);
        if (keys.length == 0) {
            return "";
        }

        // else we lookup and concatenate
        final EntityDoc[] lds = getLexiconDefinitions(keys);

        if (lds.length == 0) {
            return vocabIdentifiers;
        }

        if (lds.length == 1) {
            return provider.getData(lds[0]);
        }

        // otherwise, we need to resort to concatenating the fields
        final StringBuilder sb = new StringBuilder(lds.length * 32);
        sb.append('[');

        for (int ii = 0; ii < lds.length; ii++) {
            final EntityDoc l = lds[ii];
            sb.append(provider.getData(l));
            if (ii + 1 < lds.length) {
                sb.append(MULTI_WORD_SEPARATOR);
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * returns the lexicon definitions
     *
     * @param keys the keys to match
     * @return the lexicon definitions that were found
     */
    private EntityDoc[] getLexiconDefinitions(final String[] keys) {
        return this.definitions.searchUniqueBySingleField("strongNumber", keys);
    }

    /**
     * Extracts a compound key into several keys
     *
     * @param vocabIdentifiers the vocabulary identifiers
     * @return the list of all keys to lookup
     */
    String[] getKeys(final String vocabIdentifiers) {
        if (isBlank(vocabIdentifiers)) {
            return new String[0];
        }

        final String[] ids = vocabIdentifiers.split(STRONG_SEPARATORS);

        for (int ii = 0; ii < ids.length; ii++) {
            final char firstChar = ids[ii].charAt(0);
            if (firstChar == 'G' || firstChar == 'H') {
                ids[ii] = padStrongNumber(ids[ii], false);
            } else if ((ids[ii].startsWith(HIGHER_STRONG) || ids[ii].startsWith(LOWER_STRONG))
                    && ids[ii].length() > START_STRONG_KEY) {
                ids[ii] = padStrongNumber(ids[ii].substring(START_STRONG_KEY), false);
            }
        }
        return ids;
    }

    /**
     * Pads a strong number with the correct number of 0s
     *
     * @param strongNumber the strong number
     * @param prefix true to indicate the strongNumber is preceded with strong:
     * @return the padded strong number
     */
    public static String padStrongNumber(final String strongNumber, final boolean prefix) {
        final int baseIndex = prefix ? START_STRONG_KEY : 0;
        String subStrong = null;
        try {
            subStrong = strongNumber.substring(baseIndex + 1);
            return String.format("%c%04d", strongNumber.charAt(baseIndex), Integer.parseInt(subStrong));
        } catch (final NumberFormatException e) {
            LOGGER.trace("Unable to parse strong number.", e);
            // deals with dodgy modules
            // perhaps someone added some random information at the end
            if (subStrong != null && subStrong.length() > 3) {
                final String first4Chars = subStrong.substring(0, 4);
                try {
                    return String.format("%c%04d", strongNumber.charAt(baseIndex),
                            Integer.parseInt(first4Chars));
                } catch (final NumberFormatException ex) {
                    // couldn't convert to a padded number
                    LOGGER.trace("Unable to convert [{}] to a padded number.", first4Chars);
                    return strongNumber;
                }
            }

            return "err";
        }
    }
}
