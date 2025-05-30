/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentLruCache;

/**
 * {@link TypeInformation} for a plain {@link Class}.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class ClassTypeInformation<S> extends TypeDiscoverer<S> {

	private static final ConcurrentLruCache<ResolvableType, ClassTypeInformation<?>> cache = new ConcurrentLruCache<>(128,
			ClassTypeInformation::new);

	private static final ConcurrentLruCache<Class<?>, ResolvableType> resolvableTypeCache = new ConcurrentLruCache<>(128,
			ResolvableType::forClass);

	public static final ClassTypeInformation<Collection> COLLECTION;
	public static final ClassTypeInformation<List> LIST;
	public static final ClassTypeInformation<Set> SET;
	public static final ClassTypeInformation<Map> MAP;
	public static final ClassTypeInformation<Object> OBJECT;

	static {

		OBJECT = from(Object.class);
		COLLECTION = from(Collection.class);
		LIST = from(List.class);
		SET = from(Set.class);
		MAP = from(Map.class);
	}

	private final Class<S> type;

	ClassTypeInformation(ResolvableType type) {
		super(type);
		this.type = (Class<S>) type.resolve(Object.class);
	}

	/**
	 * @param <S>
	 * @param type
	 * @return
	 */
	public static <S> ClassTypeInformation<S> from(Class<S> type) {
		return from(resolvableTypeCache.get(type));
	}

	static <S> ClassTypeInformation<S> from(ResolvableType type) {

		Assert.notNull(type, "Type must not be null");

		return (ClassTypeInformation<S>) cache.get(type);
	}

	@Override
	public Class<S> getType() {
		return type;
	}

	@Override
	public ClassTypeInformation<?> getRawTypeInformation() {
		return this;
	}

	@Override
	public boolean isAssignableFrom(TypeInformation<?> target) {
		return getType().isAssignableFrom(target.getType());
	}

	@Override
	public TypeInformation<? extends S> specialize(TypeInformation<?> type) {
		return (TypeInformation<? extends S>) type;
	}

	@Override
	public String toString() {
		return type.getName();
	}
}
