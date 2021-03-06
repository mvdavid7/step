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
package com.tyndalehouse.step.core.service.jsword.impl;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.tyndalehouse.step.core.exceptions.LuceneSearchException;
import org.crosswire.jsword.book.Book;
import org.crosswire.jsword.book.BookException;
import org.crosswire.jsword.index.IndexStatus;
import org.crosswire.jsword.index.search.DefaultSearchModifier;
import org.crosswire.jsword.index.search.DefaultSearchRequest;
import org.crosswire.jsword.passage.*;
import org.crosswire.jsword.passage.PassageTally.Order;
import org.crosswire.jsword.versification.Versification;
import org.crosswire.jsword.versification.VersificationsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tyndalehouse.step.core.exceptions.StepInternalException;
import com.tyndalehouse.step.core.models.LookupOption;
import com.tyndalehouse.step.core.models.OsisWrapper;
import com.tyndalehouse.step.core.models.search.SearchEntry;
import com.tyndalehouse.step.core.models.search.SearchResult;
import com.tyndalehouse.step.core.models.search.VerseSearchEntry;
import com.tyndalehouse.step.core.service.impl.IndividualSearch;
import com.tyndalehouse.step.core.service.impl.SearchQuery;
import com.tyndalehouse.step.core.service.jsword.JSwordPassageService;
import com.tyndalehouse.step.core.service.jsword.JSwordSearchService;
import com.tyndalehouse.step.core.service.jsword.JSwordVersificationService;

/**
 * API to search across the data
 *
 * @author chrisburrell
 */
@Singleton
public class JSwordSearchServiceImpl implements JSwordSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSwordSearchServiceImpl.class);
    private static final int MAX_RESULTS = 50000;
    private static final Pattern GEN_REV_RANGE = Pattern.compile("(\\+)\\[Gen-Rev\\]");
    private final JSwordVersificationService av11nService;
    private final JSwordPassageService jsword;

    /**
     * @param av11nService the versification service
     * @param jsword       the jsword lookup service to retrieve the references
     */
    @Inject
    public JSwordSearchServiceImpl(final JSwordVersificationService av11nService,
                                   final JSwordPassageService jsword) {
        this.av11nService = av11nService;
        this.jsword = jsword;

    }

    @Override
    public int estimateSearchResults(final SearchQuery sq) {
        final long start = System.currentTimeMillis();

        final Key k = searchKeys(sq);
        LOGGER.trace("Took [{}]ms", System.currentTimeMillis() - start);

        return k.getCardinality();
    }

    @Override
    public Key searchKeys(final SearchQuery sq) {
        final DefaultSearchModifier modifier = new DefaultSearchModifier();

        // we have a linked hashmap, because we want to preserve the order of the versions we're looking up
        // this was we end up with the results in the correct versification
        final Map<String, Key> resultsPerVersion = new LinkedHashMap<String, Key>();
        modifier.setRanked(sq.isRanked());

        // need to set to something sensible, other we may experience a
        // "Requested array size exceeds VM limit"
        modifier.setMaxResults(MAX_RESULTS);

        final IndividualSearch currentSearch = sq.getCurrentSearch();
        for (final String version : currentSearch.getVersions()) {

            // now for each version, we do the search and store it in a map
            final Book bible = this.av11nService.getBookFromVersion(version);

            // TODO JS-228 raised for thread-safety
            synchronized (this) {
                if (bible.getIndexStatus().equals(IndexStatus.DONE)) {
                    final Key luceneSearchResults;
                    try {
                        String query = currentSearch.getQuery();
                        //small optimization and cater for versions that don't support Gen-Rev as a range:
                        query = GEN_REV_RANGE.matcher(query).replaceAll("");
                        luceneSearchResults = bible.find(new DefaultSearchRequest(query, modifier));
                    } catch (final BookException e) {
                        throw new LuceneSearchException("Unable to search for " + currentSearch.getQuery()
                                + " with Bible " + version, e);
                    }

                    resultsPerVersion.put(version, luceneSearchResults);
                } else {
                    LOGGER.error("Module [{}] is not indexed.", version);
                    resultsPerVersion.put(version, new DefaultKeyList());
                }
            }
        }

        // we then need to merge the keys together
        // otherwise, we are into the realm of searching across multiple versions
        // no need to rank, since it won't be possible to rank accurately across versions
        return mergeSearches(resultsPerVersion);
    }

    /**
     * merges all search results together
     *
     * @param resultsPerVersion the results per version
     * @return the list of results
     */
    private Key mergeSearches(final Map<String, Key> resultsPerVersion) {
        Key all = null;
        Versification allVersification = null;

        for (final Entry<String, Key> entry : resultsPerVersion.entrySet()) {
            final Key value = entry.getValue();
            LOGGER.debug("Sub-result-set [{}] has [{}] entries", entry.getKey(), value.getCardinality());

            if (all == null) {
                all = value;

                if(all instanceof VerseKey) {
                    allVersification = ((VerseKey) all).getVersification();
                }
            } else {
                boolean valueIsVerseKey = value instanceof VerseKey;
                if(valueIsVerseKey && allVersification == null) {
                    throw new StepInternalException("Trying to combine versified key with non-versified key.");
                }

                //i.e. and allVersification != null
                Key convertedKey = value;
                if(valueIsVerseKey) {
                    final VerseKey versifiedResults = (VerseKey) value;
                    final Passage versifiedPassageResults = KeyUtil.getPassage(versifiedResults);
                    convertedKey = VersificationsMapper.instance().map(versifiedPassageResults, allVersification);
                }

                all.addAll(convertedKey);
            }
        }

        LOGGER.debug("Combined result-set has [{}] entries", all.getCardinality());
        return all;
    }

    @Override
    public SearchResult search(final SearchQuery sq, final String version, final LookupOption... options) {
        return retrieveResultsFromKeys(sq, searchKeys(sq), version, options);
    }

    @Override
    public SearchResult retrieveResultsFromKeys(final SearchQuery sq, final Key results,
                                                final String version, final LookupOption... options) {
        final int total = getTotal(results);

        LOGGER.debug("Total of [{}] results.", total);
        final Key newResults = rankAndTrimResults(sq, results);
        LOGGER.debug("Trimmed down to [{}].", newResults.getCardinality());
        return getResultsFromTrimmedKeys(sq, version, total, newResults, options);


    }

    /**
     * Assumes the page size logic has already been run, retrieves results from the actual book in quest
     *
     * @param sq         the search criteria
     * @param version    the version
     * @param total      the total number of items
     * @param newResults the page of keys to retrieve
     * @param options    the options to retrieve the passage with
     * @return the search result passages
     */
    @Override
    public SearchResult getResultsFromTrimmedKeys(final SearchQuery sq, final String version, final int total, final Key newResults, final LookupOption... options) {
        final long startRefRetrieval = System.currentTimeMillis();

        // if context > 0, then we need to add verse numbers:
        final List<LookupOption> lookupOptions = new ArrayList<LookupOption>();
        Collections.addAll(lookupOptions, options);
        if (sq.getContext() > 0) {
            lookupOptions.add(LookupOption.VERSE_NUMBERS);
        }

        final Book bible = this.av11nService.getBookFromVersion(version);
        final List<SearchEntry> resultPassages = getPassagesForResults(bible, newResults, sq.getContext(),
                lookupOptions);

        return getSearchResult(resultPassages, total, System.currentTimeMillis() - startRefRetrieval);
    }

    /**
     * returns the total or -1 if not available
     *
     * @param results the key to set of results
     * @return the results
     */
    @Override
    public int getTotal(final Key results) {
        return results.getCardinality();
    }

    /**
     * Constructs the search result object
     *
     * @param resultPassages the resulting passages
     * @param total          the total number of hits
     * @param retrievalTime  the time taken to retrieve the references attached to the search results
     * @return the search result to be returned to the service caller
     */
    private SearchResult getSearchResult(final List<SearchEntry> resultPassages, final int total,
                                         final long retrievalTime) {
        final SearchResult r = new SearchResult();
        r.setResults(resultPassages);

        // set stats:
        r.setTimeTookToRetrieveScripture(retrievalTime);
        r.setTotal(total);
        return r;
    }

    /**
     * Looks up all passages represented by the key
     *
     * @param bible   the bible under examination
     * @param results the list of results
     * @param context amount of context to add
     * @param options to use to lookup the right parameterization of the text
     * @return the list of entries found
     */
    private List<SearchEntry> getPassagesForResults(final Book bible, final Key results, final int context,
                                                    final List<LookupOption> options) {
        final List<SearchEntry> resultPassages = new ArrayList<SearchEntry>();
        final Iterator<Key> iterator = ((Passage) results).iterator();

        while (iterator.hasNext()) {
            final Key verse = iterator.next();
            final Key lookupKey;

            if (verse instanceof Verse) {
                // then we need to make it into a verse range
                final Verse verseAsVerse = (Verse) verse;
                final VerseRange vr = new VerseRange(verseAsVerse.getVersification(), verseAsVerse);
                vr.blur(context, RestrictionType.NONE);
                lookupKey = vr;
            } else {
                // assume blur is supported
                verse.blur(context, RestrictionType.NONE);
                lookupKey = verse;
            }

            // TODO this is not very efficient so requires refactoring
            final OsisWrapper peakOsisText = this.jsword.peakOsisText(bible, lookupKey, options);
            resultPassages.add(new VerseSearchEntry(peakOsisText.getReference(), peakOsisText.getValue(),
                    peakOsisText.getOsisId()));
        }
        return resultPassages;
    }

    /**
     * @param sq      search query
     * @param results the result to be trimmed
     * @return the results
     */
    @Override
    public Key rankAndTrimResults(final SearchQuery sq, final Key results) {
        rankResults(sq.isRanked(), results);

        final Passage passage = (Passage) results;

        if (!sq.isAllKeys()) {

            // we need the first pageNumber*PAGE_SIZE results, so remove anything beyond that.
            passage.trimVerses(sq.getPageNumber() * sq.getPageSize());
            Passage newResults = passage;

            while (newResults.getCardinality() > sq.getPageSize()) {
                newResults = newResults.trimVerses(sq.getPageSize());
            }

            return newResults;
        }
        return results;
    }

    /**
     * Sets up the passage tally to rank the results
     *
     * @param ranked  true to indicate ranking occurs
     * @param results the results, amended to reflect what is desired
     */
    private void rankResults(final boolean ranked, final Key results) {
        if (ranked) {
            if (!(results instanceof PassageTally)) {
                throw new StepInternalException("Unable to retrieve in ranked order...");
            }

            ((PassageTally) results).setOrdering(Order.TALLY);

        }
    }
}
