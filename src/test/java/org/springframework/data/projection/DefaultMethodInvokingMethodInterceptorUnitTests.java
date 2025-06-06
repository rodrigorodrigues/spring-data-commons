/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.projection;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

/**
 * Unit tests for {@link DefaultMethodInvokingMethodInterceptor}.
 *
 * @author Mark Paluch
 * @author Oliver Gierke
 */
class DefaultMethodInvokingMethodInterceptorUnitTests {

	@Test // GH-2971
	void invokesDefaultMethodOnProxy() {

		ProxyFactory factory = new ProxyFactory();
		factory.setInterfaces(Sample.class);
		factory.addAdvice(new DefaultMethodInvokingMethodInterceptor());

		Object proxy = factory.getProxy();

		assertThat(proxy).isInstanceOfSatisfying(Sample.class, it -> {
			assertThat(it.sample()).isEqualTo("sample");
		});
	}

	interface Sample {

		default String sample() {
			return "sample";
		}
	}
}
