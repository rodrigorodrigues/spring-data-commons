/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.data.aot;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.javapoet.TypeSpec.Builder;
import org.springframework.util.Assert;

/**
 * @author Christoph Strobl
 */
public class DeferredTypeBuilder implements Consumer<Builder> {

	@Nullable
	private Consumer<Builder> type;

	@Override
	public void accept(Builder type) {
		Assert.notNull(this.type, "No type builder set");
		this.type.accept(type);
	}

	public void set(Consumer<Builder> type) {
		this.type = type;
	}
}
