/*
 * Copyright 2021-2025 the original author or authors.
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
package org.springframework.data.repository.query;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.lang.CheckReturnValue;
import org.springframework.lang.Contract;

/**
 * Fluent interface to define and run a query along with projection and sorting and. Instances of {@link FluentQuery}
 * are immutable.
 *
 * @author Mark Paluch
 * @since 2.6
 */
public interface FluentQuery<T> {

	/**
	 * Define the sort order. Multiple calls will add {@link Sort#and(Sort) Sort} specifications.
	 *
	 * @param sort the {@link Sort} specification to sort the results by, may be {@link Sort#unsorted()}, must not be
	 *          {@literal null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if {@code sort} is {@code null}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	FluentQuery<T> sortBy(Sort sort);

	/**
	 * Define the query limit.
	 *
	 * @param limit the limit to apply to the query to limit results. Must not be negative.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if {@code limit} is less than zero.
	 * @throws UnsupportedOperationException if not supported by the underlying implementation.
	 * @since 3.1
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default FluentQuery<T> limit(int limit) {
		throw new UnsupportedOperationException("Limit not supported");
	}

	/**
	 * Define the target type the result should be mapped to. Skip this step if you are only interested in the original
	 * domain type.
	 *
	 * @param resultType must not be {@code null}.
	 * @param <R> result type.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if {@code resultType} is {@code null}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	<R> FluentQuery<R> as(Class<R> resultType);

	/**
	 * Define which properties or property paths to include in the query.
	 *
	 * @param properties must not be {@code null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if {@code properties} is {@code null}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	default FluentQuery<T> project(String... properties) {
		return project(Arrays.asList(properties));
	}

	/**
	 * Define which properties or property paths to include in the query.
	 *
	 * @param properties must not be {@code null}.
	 * @return a new instance of {@link FluentQuery}.
	 * @throws IllegalArgumentException if {@code properties} is {@code null}.
	 */
	@Contract("_ -> new")
	@CheckReturnValue
	FluentQuery<T> project(Collection<String> properties);

	/**
	 * Fetchable extension {@link FluentQuery} allowing to materialize results from the underlying query.
	 *
	 * @author Mark Paluch
	 * @since 2.6
	 */
	interface FetchableFluentQuery<T> extends FluentQuery<T> {

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		FetchableFluentQuery<T> sortBy(Sort sort);

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		default FetchableFluentQuery<T> limit(int limit) {
			throw new UnsupportedOperationException("Limit not supported");
		}

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		<R> FetchableFluentQuery<R> as(Class<R> resultType);

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		default FetchableFluentQuery<T> project(String... properties) {
			return project(Arrays.asList(properties));
		}

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		FetchableFluentQuery<T> project(Collection<String> properties);

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@link Optional#empty()} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		default Optional<T> one() {
			return Optional.ofNullable(oneValue());
		}

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@literal null} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		@Nullable
		T oneValue();

		/**
		 * Get the first or no result.
		 *
		 * @return {@link Optional#empty()} if no match found.
		 */
		default Optional<T> first() {
			return Optional.ofNullable(firstValue());
		}

		/**
		 * Get the first or no result.
		 *
		 * @return {@literal null} if no match found.
		 */
		@Nullable
		T firstValue();

		/**
		 * Get all matching elements.
		 *
		 * @return
		 */
		List<T> all();

		/**
		 * Get all matching elements as {@link Window} to start result scrolling or resume scrolling at
		 * {@code scrollPosition}.
		 *
		 * @param scrollPosition must not be {@literal null}.
		 * @throws IllegalArgumentException if {@code scrollPosition} is {@literal null}.
		 * @throws UnsupportedOperationException if not supported by the underlying implementation.
		 * @since 3.1
		 */
		default Window<T> scroll(ScrollPosition scrollPosition) {
			throw new UnsupportedOperationException("Scrolling not supported");
		}

		/**
		 * Get a page of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort}.
		 *          Any potentially specified {@link #limit(int)} will be overridden by {@link Pageable#getPageSize()}.
		 */
		Page<T> page(Pageable pageable);

		/**
		 * Get a slice of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a sliced result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort}.
		 *          Any potentially specified {@link #limit(int)} will be overridden by {@link Pageable#getPageSize()}.
		 * @return
		 * @since 3.5
		 */
		default Slice<T> slice(Pageable pageable) {
			return page(pageable);
		}

		/**
		 * Stream all matching elements.
		 *
		 * @return a {@link Stream} wrapping cursors that need to be closed.
		 */
		Stream<T> stream();

		/**
		 * Get the number of matching elements.
		 *
		 * @return total number of matching elements.
		 */
		long count();

		/**
		 * Check for the presence of matching elements.
		 *
		 * @return {@literal true} if at least one matching element exists.
		 */
		boolean exists();
	}

	/**
	 * Reactive extension {@link FluentQuery} allowing to materialize results from the underlying query.
	 *
	 * @author Mark Paluch
	 * @since 2.6
	 */
	interface ReactiveFluentQuery<T> extends FluentQuery<T> {

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		ReactiveFluentQuery<T> sortBy(Sort sort);

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		default ReactiveFluentQuery<T> limit(int limit) {
			throw new UnsupportedOperationException("Limit not supported");
		}

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		<R> ReactiveFluentQuery<R> as(Class<R> resultType);

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		default ReactiveFluentQuery<T> project(String... properties) {
			return project(Arrays.asList(properties));
		}

		@Override
		@Contract("_ -> new")
		@CheckReturnValue
		ReactiveFluentQuery<T> project(Collection<String> properties);

		/**
		 * Get exactly zero or one result.
		 *
		 * @return {@code null} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more than one match found.
		 */
		Mono<T> one();

		/**
		 * Get the first or no result.
		 *
		 * @return {@code null} if no match found.
		 */
		Mono<T> first();

		/**
		 * Get all matching elements.
		 */
		Flux<T> all();

		/**
		 * Get all matching elements as {@link Window} to start result scrolling or resume scrolling at
		 * {@code scrollPosition}.
		 *
		 * @param scrollPosition must not be {@literal null}.
		 * @throws IllegalArgumentException if {@code scrollPosition} is {@literal null}.
		 * @throws UnsupportedOperationException if not supported by the underlying implementation.
		 * @since 3.1
		 */
		default Mono<Window<T>> scroll(ScrollPosition scrollPosition) {
			throw new UnsupportedOperationException("Scrolling not supported");
		}

		/**
		 * Get a page of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a paged result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort}.
		 *          Any potentially specified {@link #limit(int)} will be overridden by {@link Pageable#getPageSize()}.
		 */
		Mono<Page<T>> page(Pageable pageable);

		/**
		 * Get a slice of matching elements for {@link Pageable}.
		 *
		 * @param pageable the pageable to request a sliced result, can be {@link Pageable#unpaged()}, must not be
		 *          {@literal null}. The given {@link Pageable} will override any previously specified {@link Sort sort}.
		 *          Any potentially specified {@link #limit(int)} will be overridden by {@link Pageable#getPageSize()}.
		 * @return
		 * @since 3.5
		 */
		default Mono<Slice<T>> slice(Pageable pageable) {
			return page(pageable).map(Function.identity());
		}

		/**
		 * Get the number of matching elements.
		 *
		 * @return total number of matching elements.
		 */
		Mono<Long> count();

		/**
		 * Check for the presence of matching elements.
		 *
		 * @return {@literal true} if at least one matching element exists.
		 */
		Mono<Boolean> exists();

	}
}
