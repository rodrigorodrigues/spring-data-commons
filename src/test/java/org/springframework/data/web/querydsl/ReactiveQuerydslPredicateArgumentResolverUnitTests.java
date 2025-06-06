/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.web.querydsl;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.custom.querydslpredicatebuilder.QuerydslPredicateBuilderCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.SimplePath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.querydsl.QUser;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.binding.*;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import com.querydsl.core.types.Predicate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * Unit tests for {@link ReactiveQuerydslPredicateArgumentResolver}.
 *
 * @author Mark Paluch
 */
class ReactiveQuerydslPredicateArgumentResolverUnitTests {

	ReactiveQuerydslPredicateArgumentResolver resolver;

	@BeforeEach
	void setUp() {

		resolver = new ReactiveQuerydslPredicateArgumentResolver(
				new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE), DefaultConversionService.getSharedInstance());
	}

	@Test // DATACMNS-1785
	void supportsParameterReturnsTrueWhenMethodParameterIsPredicateButNotAnnotatedAsSuch() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("predicateWithoutAnnotation", Predicate.class)))
				.isTrue();
	}

	@Test // DATACMNS-1785
	void supportsParameterShouldThrowExceptionWhenMethodParameterIsNoPredicateButAnnotatedAsSuch() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> resolver.supportsParameter(getMethodParameterFor("nonPredicateWithAnnotation", String.class)));
	}

	@Test // DATACMNS-1785
	void supportsParameterReturnsFalseWhenMethodParameterIsNoPredicate() {
		assertThat(resolver.supportsParameter(getMethodParameterFor("nonPredicateWithoutAnnotation", String.class)))
				.isFalse();
	}

	@Test // DATACMNS-1785
	void resolveArgumentShouldCreateSingleStringParameterPredicateCorrectly() {

		var request = MockServerHttpRequest.get("").queryParam("firstname", "rand").build();

		var predicate = resolver.resolveArgumentValue(getMethodParameterFor("simpleFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand"));
	}

	@Test // DATACMNS-1785
	void resolveArgumentShouldHonorCustomSpecification() {

		var request = MockServerHttpRequest.get("").queryParam("firstname", "egwene")
				.queryParam("lastname", "al'vere").build();

		var predicate = resolver.resolveArgumentValue(getMethodParameterFor("specificFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate).isEqualTo(
				QUser.user.firstname.eq("egwene".toUpperCase()).and(QUser.user.lastname.toLowerCase().eq("al'vere")));
	}

	@Test // DATACMNS-1785
	void returnsEmptyPredicateForEmptyInput() {

		var parameter = getMethodParameterFor("predicateWithoutAnnotation", Predicate.class);

		var request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
				.isNotNull();
	}

	@Test // DATACMNS-1785
	void forwardsNullValueForNullablePredicate() {

		var parameter = getMethodParameterFor("nullablePredicateWithoutAnnotation", Predicate.class);

		var request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))).isNull();
	}

	@Test // DATACMNS-1785
	void returnsOptionalIfDeclared() {

		var parameter = getMethodParameterFor("optionalPredicateWithoutAnnotation", Optional.class);

		var request = MockServerHttpRequest.get("").queryParam("firstname", "").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).isEmpty());

		request = MockServerHttpRequest.get("").queryParam("firstname", "Matthews").build();

		assertThat(resolver.resolveArgumentValue(parameter, null, MockServerWebExchange.from(request))) //
				.isInstanceOfSatisfying(Optional.class, it -> assertThat(it).isPresent());
	}

	@Test
	void resolveArgumentCustomQuerydslPredicateBuilder() throws Exception {
		QuerydslPredicateBuilder querydslPredicateBuilder = new QuerydslPredicateBuilderCustom();

		var request = MockServerHttpRequest.get("")
				.queryParam("firstname", "rand")
				.queryParam("lastname", "something")
				.build();

		ReactiveQuerydslPredicateArgumentResolver resolverQuerydslPredicateBuilder = new ReactiveQuerydslPredicateArgumentResolver(new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE), querydslPredicateBuilder);

		var predicate = resolverQuerydslPredicateBuilder.resolveArgumentValue(getMethodParameterFor("simpleFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		// Using new querydslPredicateBuilder predicate would be with OR operator.
		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand").or(QUser.user.lastname.eq("something")));

		// Using the default predicate is with AND operator.
		predicate = resolver.resolveArgumentValue(getMethodParameterFor("simpleFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate).isEqualTo(QUser.user.firstname.eq("rand").and(QUser.user.lastname.eq("something")));

		//Apply something fancy
		querydslPredicateBuilder = new QuerydslPredicateBuilder(DefaultConversionService.getSharedInstance(), new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE).getEntityPathResolver()) {
			@Override
			public Predicate getPredicate(TypeInformation<?> type, MultiValueMap<String, ?> values, QuerydslBindings bindings) {
				BooleanBuilder builder = new BooleanBuilder();
				SimplePath<QUser> pathUser = Expressions.path(QUser.class, "user");
				for (var entry : values.entrySet()) {
					Path<String> path = ExpressionUtils.path(String.class, pathUser, entry.getKey());
					builder.or(Expressions.predicate(Ops.STARTS_WITH_IC, path, Expressions.constant(entry.getValue())));
				}
				return builder.getValue();
			}
		};

		resolverQuerydslPredicateBuilder = new ReactiveQuerydslPredicateArgumentResolver(new QuerydslBindingsFactory(SimpleEntityPathResolver.INSTANCE), querydslPredicateBuilder);

		predicate = resolverQuerydslPredicateBuilder.resolveArgumentValue(getMethodParameterFor("simpleFind", Predicate.class), null,
				MockServerWebExchange.from(request));

		assertThat(predicate.toString()).isEqualTo(QUser.user.firstname.startsWithIgnoreCase("[rand]").or(QUser.user.lastname.startsWithIgnoreCase("[something]")).toString());
	}

	private static MethodParameter getMethodParameterFor(String methodName, Class<?>... args) throws RuntimeException {

		try {
			return new MethodParameter(Sample.class.getMethod(methodName, args), args.length == 0 ? -1 : 0);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	static class SpecificBinding implements QuerydslBinderCustomizer<QUser> {

		@Override
		public void customize(QuerydslBindings bindings, QUser user) {

			bindings.bind(user.firstname).firstOptional((path, value) -> value.map(it -> path.eq(it.toUpperCase())));
			bindings.bind(user.lastname).first((path, value) -> path.toLowerCase().eq(value));

			bindings.excluding(user.address);
		}
	}

	interface Sample {

		User predicateWithoutAnnotation(Predicate predicate);

		User nonPredicateWithAnnotation(@QuerydslPredicate String predicate);

		User nonPredicateWithoutAnnotation(String predicate);

		User simpleFind(@QuerydslPredicate Predicate predicate);

		User specificFind(@QuerydslPredicate(bindings = SpecificBinding.class) Predicate predicate);

		// Nullability

		User nullablePredicateWithoutAnnotation(@Nullable Predicate predicate);

		User optionalPredicateWithoutAnnotation(Optional<Predicate> predicate);
	}

}
