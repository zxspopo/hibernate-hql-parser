/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.query.lucene.test.internal.builder;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.lucene.search.Query;
import org.hibernate.query.lucene.internal.builder.LuceneQueryBuilder;
import org.hibernate.query.lucene.internal.builder.PropertyHelper;
import org.hibernate.query.lucene.test.internal.builder.model.IndexedEntity;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.test.programmaticmapping.TestingSearchFactoryHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test for {@link LuceneQueryBuilder}.
 *
 * @author Gunnar Morling
 */
public class LuceneQueryBuilderTest {

	@Rule
	public TestingSearchFactoryHolder factoryHolder = new TestingSearchFactoryHolder( IndexedEntity.class );

	private LuceneQueryBuilder queryBuilder;

	@Before
	public void setupQueryBuilder() {
		SearchFactoryIntegrator searchFactory = factoryHolder.getSearchFactory();
		PropertyHelper propertyTypeHelper = new PropertyHelper( searchFactory );
		QueryContextBuilder queryContextBuilder = searchFactory.buildQueryBuilder();

		queryBuilder = new LuceneQueryBuilder( queryContextBuilder, propertyTypeHelper );
	}

	@Test
	public void shouldBuildEqualsQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.addEqualsPredicate( "name", "foobar" )
			.build();

		assertThat( query.toString() ).isEqualTo( "name:foobar" );
	}

	@Test
	public void shouldBuildLongEqualsQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.addEqualsPredicate( "l", "10" )
			.build();

		assertThat( query.toString() ).isEqualTo( "l:[10 TO 10]" );
	}

	@Test
	public void shouldBuildDoubleEqualsQuery() {
		Query query = queryBuilder
				.setEntityType( IndexedEntity.class )
				.addEqualsPredicate( "d", "10.0" )
				.build();

		assertThat( query.toString() ).isEqualTo( "d:[10.0 TO 10.0]" );
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldFailQueryOnAnalyzedField() {
		queryBuilder
				.setEntityType( IndexedEntity.class )
				.addEqualsPredicate( "description", "foo" )
				.build();
	}

	@Test
	public void shouldBuildRangeQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.addRangePredicate( "i", "1", "10" )
			.build();

		assertThat( query.toString() ).isEqualTo( "i:[1 TO 10]" );
	}

	@Test
	public void shouldBuildConjunctionQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.pushAndPredicate()
				.addEqualsPredicate( "name", "foobar" )
				.addEqualsPredicate( "i", "1" )
			.build();

		assertThat( query.toString() ).isEqualTo( "+name:foobar +i:[1 TO 1]" );
	}

	@Test
	public void shouldBuildDisjunctionQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.pushOrPredicate()
				.addEqualsPredicate( "name", "foobar" )
				.addEqualsPredicate( "i", "1" )
			.build();

		assertThat( query.toString() ).isEqualTo( "name:foobar i:[1 TO 1]" );
	}

	@Test
	public void shouldBuildNegationQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.pushNotPredicate()
				.addEqualsPredicate( "name", "foobar" )
			.build();

		assertThat( query.toString() ).isEqualTo( "-name:foobar *:*" );
	}

	@Test
	public void shouldBuildNestedLogicalPredicatesQuery() {
		Query query = queryBuilder
			.setEntityType( IndexedEntity.class )
			.pushAndPredicate()
				.pushOrPredicate()
					.addEqualsPredicate( "name", "foobar" )
					.addEqualsPredicate( "i", "1" )
					.popBooleanPredicate()
				.addEqualsPredicate( "l", "10" )
			.build();

		assertThat( query.toString() ).isEqualTo( "+(name:foobar i:[1 TO 1]) +l:[10 TO 10]" );
	}
}