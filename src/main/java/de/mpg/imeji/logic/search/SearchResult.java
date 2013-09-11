/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.Jena;

import de.mpg.imeji.logic.search.vo.SearchIndex;
import de.mpg.imeji.logic.search.vo.SortCriterion;
import de.mpg.j2j.helper.SortHelper;

/**
 * Result {@link Object} for {@link Search}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchResult
{
    private int numberOfRecords = 0;
    private List<String> results = new ArrayList<String>();
    private String query = null;
    private SortCriterion sort = new SortCriterion();

    /**
     * Create a new {@link SearchResult} from a {@link List} of String, and sort it if a {@link SortCriterion} has been
     * defined <br/>
     * Sorting not made on {@link Jena} level, for performance purpose
     * 
     * @param unsortedResults
     * @param sort
     */
    public SearchResult(List<String> unsortedResults, SortCriterion sort)
    {
        numberOfRecords = unsortedResults.size();
        if (sort != null)
        {
            if (sort.getIndex() != null && sort.getIndex().getName().equals(SearchIndex.names.cont_title.name()))
                sort.toggle();
            this.sort = sort;
        }
        results = SortHelper.sort(unsortedResults, this.sort.getSortOrder());
    }

    public int getNumberOfRecords()
    {
        return numberOfRecords;
    }

    public void setNumberOfRecords(int numberOfRecords)
    {
        this.numberOfRecords = numberOfRecords;
    }

    public List<String> getResults()
    {
        return results;
    }

    public void setResults(List<String> results)
    {
        this.results = results;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public SortCriterion getSort()
    {
        return sort;
    }

    public void setSort(SortCriterion sort)
    {
        this.sort = sort;
    }
}
