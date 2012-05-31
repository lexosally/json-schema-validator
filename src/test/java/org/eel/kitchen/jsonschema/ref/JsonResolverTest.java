/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.ref;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.eel.kitchen.jsonschema.main.JsonSchemaException;
import org.eel.kitchen.jsonschema.schema.SchemaNode;
import org.eel.kitchen.jsonschema.uri.URIManager;
import org.eel.kitchen.util.JsonLoader;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public final class JsonResolverTest
{
    /*
     * Tests:
     *
     * - no ref returns equivalent document
     * - malformed ref returns equivalent document
     * - local ref never calls URIManager
     * - ref: # always loops
     * - local ref loop is detected
     * - cross schema loop is detected
     * - when travelling through s1 -> s2 -> s1, getting s1 is only called once
     */

    private final JsonNodeFactory factory = JsonNodeFactory.instance;

    private URIManager manager;
    private JsonNode testData;

    @BeforeClass
    public void initializeTestData()
        throws IOException
    {
        testData = JsonLoader.fromResource("/ref/jsonresolver.json");
    }

    @BeforeMethod
    public void initManager()
    {
        manager = mock(URIManager.class);
    }

    private Iterator<Object[]> getReferencingData(final String name)
    {
        final JsonNode data = testData.get(name);
        final Set<Object[]> set = new HashSet<Object[]>();
        Object[] array;

        for (final JsonNode node: data) {
            array = new Object[] {
                node.get("schema"),
                node.get("expected"),
                node.get("msg").textValue()
            };
            set.add(array);
        }

        return set.iterator();

    }
    @DataProvider
    public Iterator<Object[]> singleReferencingData()
    {
        return getReferencingData("singleReferencing");
    }

    @Test(dataProvider = "singleReferencingData")
    public void testSingleReferencing(final JsonNode schema,
        final JsonNode expected, final String msg)
        throws JsonSchemaException
    {
        final JsonResolver resolver = new JsonResolver(manager);
        final SchemaContainer container = new SchemaContainer(schema);
        final SchemaNode schemaNode = new SchemaNode(container, schema);

        final SchemaNode resolved = resolver.resolve(schemaNode);

        assertEquals(resolved.getNode(), expected, msg);
    }

    @DataProvider
    public Iterator<Object[]> multiReferencingData()
    {
        return getReferencingData("multiReferencing");
    }

    @Test(dataProvider = "multiReferencingData")
    public void testMultiReferencing(final JsonNode schema,
        final JsonNode expected, final String msg)
        throws JsonSchemaException
    {
        final JsonResolver resolver = new JsonResolver(manager);
        final SchemaContainer container = new SchemaContainer(schema);
        final SchemaNode schemaNode = new SchemaNode(container, schema);

        final SchemaNode resolved = resolver.resolve(schemaNode);

        assertEquals(resolved.getNode(), expected, msg);
    }

    @DataProvider
    public Iterator<Object[]> loopData()
    {
        final JsonNode data = testData.get("loops");
        final Set<Object[]> set = new HashSet<Object[]>();
        Object[] array;

        for (final JsonNode node: data) {
            array = new Object[] {
                node.get("schema"),
                node.get("msg").textValue()
            };
            set.add(array);
        }

        return set.iterator();
    }

    @Test(
        timeOut = 5000,
        dataProvider = "loopData"
    )
    public void testLoopDetection(final JsonNode schema, final String msg)
        throws JsonSchemaException
    {
        final SchemaContainer container = new SchemaContainer(schema);
        final SchemaNode schemaNode = new SchemaNode(container, schema);

        final JsonResolver resolver = new JsonResolver(manager);

        try {
            resolver.resolve(schemaNode);
            fail("No excpetion thrown!");
        } catch (JsonSchemaException e) {
            assertEquals(e.getMessage(), "ref loop detected", msg);
        }
    }
}