/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe Systems Incorporated
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.Collections;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.adobe.cq.sightly.WCMBindings;
import com.adobe.cq.wcm.core.components.Utils;
import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.models.List;
import com.day.cq.search.SimpleSearch;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.designer.Style;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ListImplTest {

    private static final String TEST_BASE = "/list";
    private static final String CURRENT_PAGE = "/content/list";
    private static final String LIST_1 = "/content/list/listTypes/staticListType";
    private static final String LIST_2 = "/content/list/listTypes/staticListType";
    private static final String LIST_3 = "/content/list/listTypes/childrenListType";
    private static final String LIST_4 = "/content/list/listTypes/childrenListTypeWithDepth";
    private static final String LIST_5 = "/content/list/listTypes/tagsListType";
    private static final String LIST_6 = "/content/list/listTypes/searchListType";
    private static final String LIST_7 = "/content/list/listTypes/staticOrderByTitleListType";
    private static final String LIST_8 = "/content/list/listTypes/staticOrderByTitleDescListType";
    private static final String LIST_9 = "/content/list/listTypes/staticOrderByModificationDateListType";
    private static final String LIST_10 = "/content/list/listTypes/staticOrderByModificationDateDescListType";
    private static final String LIST_11 = "/content/list/listTypes/staticMaxItemsListType";
    private static final String LIST_12 = "/content/list/listTypes/staticOrderByModificationDateListTypeWithNoModificationDate";
    private static final String LIST_13 = "/content/list/listTypes/staticOrderByModificationDateListTypeWithNoModificationDateForOneItem";
    private static final String LIST_14 = "/content/list/listTypes/staticOrderByTitleListTypeWithNoTitle";
    private static final String LIST_15 = "/content/list/listTypes/staticOrderByTitleListTypeWithNoTitleForOneItem";

    @ClassRule
    public static final AemContext CONTEXT = CoreComponentTestContext.createContext(TEST_BASE, "/content/list");

    @BeforeClass
    public static void setUp() throws Exception {
        CONTEXT.load().json("/list/test-etc.json", "/etc/tags/list");
    }

    @Test
    public void testProperties() throws Exception {
        List list = getListUnderTest(LIST_1);
        assertTrue(list.showDescription());
        assertTrue(list.showModificationDate());
        assertTrue(list.linkItems());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_1));
    }

    @Test
    public void testStaticListType() {
        List list = getListUnderTest(LIST_2);
        assertEquals(2, list.getItems().size());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_2));
    }

    @Test
    public void testChildrenListType() throws Exception {
        List list = getListUnderTest(LIST_3);
        assertEquals(3, list.getItems().size());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_3));
    }

    @Test
    public void testChildrenListTypeWithDepth() throws Exception {
        List list = getListUnderTest(LIST_4);
        assertEquals(4, list.getItems().size());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_4));
    }

    @Test
    public void testTagsListType() throws Exception {
        List list = getListUnderTest(LIST_5);
        assertEquals(1, list.getItems().size());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_5));
    }

    @Test
    public void testSearchListType() throws Exception {
        Session mockSession = mock(Session.class);
        SimpleSearch mockSimpleSearch = mock(SimpleSearch.class);
        CONTEXT.registerAdapter(ResourceResolver.class, Session.class, mockSession);
        CONTEXT.registerAdapter(Resource.class, SimpleSearch.class, mockSimpleSearch);
        SearchResult searchResult = mock(SearchResult.class);
        Hit hit = mock(Hit.class);

        when(mockSimpleSearch.getResult()).thenReturn(searchResult);
        when(searchResult.getHits()).thenReturn(Collections.singletonList(hit));
        Resource contentResource = CONTEXT.resourceResolver().getResource("/content/list/pages/page_1/jcr:content");
        when(hit.getResource()).thenReturn(contentResource);

        List list = getListUnderTest(LIST_6);
        assertEquals(1, list.getItems().size());
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_6));
    }

    @Test
    public void testOrderBy() throws Exception {
        List list = getListUnderTest(LIST_7);
        checkListConsistency(list, new String[]{"Page 1", "Page 2"});
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_7));
    }

    @Test
    public void testOrderDescBy() throws Exception {
        List list = getListUnderTest(LIST_8);
        checkListConsistency(list, new String[]{"Page 2", "Page 1"});
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_8));
    }

    @Test
    public void testOrderByModificationDate() throws Exception {
        List list = getListUnderTest(LIST_9);
        checkListConsistency(list, new String[]{"Page 2", "Page 1"});
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_9));
    }

    @Test
    public void testOrderByModificationDateDesc() throws Exception {
        List list = getListUnderTest(LIST_10);
        checkListConsistency(list, new String[]{"Page 1", "Page 2"});
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_10));
    }

    @Test
    public void testMaxItems() throws Exception {
        List list = getListUnderTest(LIST_11);
        checkListConsistency(list, new String[]{"Page 1"});
        Utils.testJSONExport(list, Utils.getTestExporterJSONPath(TEST_BASE, LIST_11));
    }

    @Test
    public void testOrderByModificationDateWithNoModificationDate() throws Exception {
        List list = getListUnderTest(LIST_12);
        checkListConsistency(list, new String[]{"Page 1.1", "Page 1.2"});
    }

    @Test
    public void testOrderByModificationDateWithNoModificationDateForOneItem() throws Exception {
        List list = getListUnderTest(LIST_13);
        checkListConsistency(list, new String[]{"Page 2", "Page 1", "Page 1.2"});
    }

    @Test
    public void testOrderByTitleWithNoTitle() throws Exception {
        List list = getListUnderTest(LIST_14);
        checkListConsistencyByPaths(list, new String[]{"/content/list/pages/page_3", "/content/list/pages/page_4"});
    }

    @Test
    public void testOrderByTitleWithNoTitleForOneItem() throws Exception {
        List list = getListUnderTest(LIST_15);
        checkListConsistencyByPaths(list, new String[]{"/content/list/pages/page_1", "/content/list/pages/page_2", "/content/list/pages/page_4"});
    }

    private List getListUnderTest(String resourcePath) {
        Resource resource = CONTEXT.resourceResolver().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Did you forget to defines test resource " + resourcePath + "?");
        }
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(CONTEXT.resourceResolver(), CONTEXT.bundleContext());
        request.setResource(resource);
        SlingBindings bindings = new SlingBindings();
        bindings.put(SlingBindings.RESOURCE, resource);
        bindings.put(SlingBindings.REQUEST, request);
        bindings.put(WCMBindings.PROPERTIES, resource.getValueMap());
        Style style = mock(Style.class);
        when(style.get(any(), any(Object.class))).thenAnswer(
                invocation -> invocation.getArguments()[1]
        );
        bindings.put(WCMBindings.CURRENT_STYLE, style);
        bindings.put(WCMBindings.CURRENT_PAGE, CONTEXT.pageManager().getPage(CURRENT_PAGE));
        request.setAttribute(SlingBindings.class.getName(), bindings);
        return request.adaptTo(List.class);
    }

    private void checkListConsistency(List list, String[] expectedPages) {
        assertTrue("Expected that the returned list will contain " + expectedPages.length + " items",
                list.getItems().size() == expectedPages.length);
        int index = 0;
        for (Page item : list.getItems()) {
            assertEquals(expectedPages[index++], item.getTitle());
        }
    }

    private void checkListConsistencyByPaths(List list, String[] expectedPagePaths) {
        assertTrue("Expected that the returned list will contain " + expectedPagePaths.length + " items",
                list.getItems().size() == expectedPagePaths.length);
        int index = 0;
        for (Page item : list.getItems()) {
            assertEquals(expectedPagePaths[index++], item.getPath());
        }
    }
}
