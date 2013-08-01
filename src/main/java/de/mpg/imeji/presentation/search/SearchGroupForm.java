/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import java.util.ArrayList;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.vo.SearchElement;
import de.mpg.imeji.logic.search.vo.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.vo.SearchGroup;
import de.mpg.imeji.logic.search.vo.SearchIndex;
import de.mpg.imeji.logic.search.vo.SearchLogicalRelation;
import de.mpg.imeji.logic.search.vo.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.vo.SearchOperators;
import de.mpg.imeji.logic.search.vo.SearchPair;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * A {@link SearchGroupForm} is a group of {@link SearchMetadataForm}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchGroupForm
{
    private List<SearchMetadataForm> elements;
    private String collectionId;
    private List<SelectItem> statementMenu;
    private static Logger logger = Logger.getLogger(SearchGroupForm.class);

    /**
     * Default Constructor
     */
    public SearchGroupForm()
    {
        elements = new ArrayList<SearchMetadataForm>();
        statementMenu = new ArrayList<SelectItem>();
    }

    /**
     * Constructor for a {@link SearchGroup} and {@link MetadataProfile}
     * 
     * @param searchGroup
     * @param profile
     * @param collectionId
     */
    public SearchGroupForm(SearchGroup searchGroup, MetadataProfile profile, String collectionId)
    {
        this();
        this.collectionId = collectionId;
        for (SearchElement se : searchGroup.getElements())
        {
            if (se.getType().equals(SEARCH_ELEMENTS.GROUP))
            {
                // Go through the search group with the collection
                for (SearchElement mde : se.getElements())
                {
                    // Add the group with the metadata
                    if (mde.getType().equals(SEARCH_ELEMENTS.GROUP))
                    {
                        elements.add(new SearchMetadataForm((SearchGroup)mde, profile));
                    }
                    else if (elements.size() > 0 && mde.getType().equals(SEARCH_ELEMENTS.LOGICAL_RELATIONS))
                    {
                        elements.get(elements.size() - 1).setLogicalRelation(
                                ((SearchLogicalRelation)mde).getLogicalRelation());
                    }
                }
            }
        }
        initStatementsMenu(profile);
    }

    /**
     * Return the {@link SearchGroupForm} as a {@link SearchGroup}
     * 
     * @return
     */
    public SearchGroup getAsSearchGroup()
    {
        SearchGroup searchGroup = new SearchGroup();
        searchGroup
                .addPair(new SearchPair(Search.getIndex(SearchIndex.names.col), SearchOperators.EQUALS, collectionId));
        searchGroup.addLogicalRelation(LOGICAL_RELATIONS.AND);
        SearchGroup groupWithAllMetadata = new SearchGroup();
        for (SearchMetadataForm e : elements)
        {
            groupWithAllMetadata.addGroup(e.getAsSearchGroup());
            groupWithAllMetadata.addLogicalRelation(e.getLogicalRelation());
        }
        searchGroup.addGroup(groupWithAllMetadata);
        return searchGroup;
    }

    /**
     * Initialize the {@link Statement} for the select menu in the form
     * 
     * @param p
     */
    public void initStatementsMenu(MetadataProfile p)
    {
        if (p.getStatements() != null)
        {
            for (Statement st : p.getStatements())
            {
                String stName = ((MetadataLabels)BeanHelper.getSessionBean(MetadataLabels.class))
                        .getInternationalizedLabels().get(st.getId());
                statementMenu.add(new SelectItem(st.getId().toString(), stName));
            }
        }
    }

    public int getSize()
    {
        return elements.size();
    }

    public List<SearchMetadataForm> getSearchElementForms()
    {
        return elements;
    }

    public void setSearchElementForms(List<SearchMetadataForm> elements)
    {
        this.elements = elements;
    }

    public String getCollectionId()
    {
        return collectionId;
    }

    public void setCollectionId(String collectionId)
    {
        this.collectionId = collectionId;
    }

    public List<SelectItem> getStatementMenu()
    {
        return statementMenu;
    }

    public void setStatementMenu(List<SelectItem> statementMenu)
    {
        this.statementMenu = statementMenu;
    }
}
