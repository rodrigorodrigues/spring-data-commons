/*
 * Copyright 2015-2025 the original author or authors.
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
package org.springframework.data.web.config;

import com.querydsl.core.types.Predicate;
import org.springframework.data.querydsl.User;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.ProjectedPayload;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oliver Gierke
 */
@Controller
class SampleController {

	@RequestMapping("/proxy")
	String someMethod(SampleDto sampleDto) {

		assertThat(sampleDto).isNotNull();
		assertThat(sampleDto.getName()).isEqualTo("Foo");
		assertThat(sampleDto.getDate()).isNotNull();

		var shippingAddresses = sampleDto.getShippingAddresses();

		assertThat(shippingAddresses).hasSize(1);
		assertThat(shippingAddresses.iterator().next().getZipCode()).isEqualTo("ZIP");
		assertThat(shippingAddresses.iterator().next().getCity()).isEqualTo("City");

		assertThat(sampleDto.getBillingAddress()).isNotNull();
		assertThat(sampleDto.getBillingAddress().getZipCode()).isEqualTo("ZIP");
		assertThat(sampleDto.getBillingAddress().getCity()).isEqualTo("City");

		return "view";
	}

	@RequestMapping("/predicate")
	@ResponseBody
	String generateCustomPredicate(@QuerydslPredicate(root = User.class) Predicate predicate) {
		return predicate.toString();
	}

	@GetMapping("/predicateMono")
	@ResponseBody
	Mono<String> generateCustomPredicateMono(@QuerydslPredicate(root = User.class) Predicate predicate) {
		return Mono.just(generateCustomPredicate(predicate));
	}

	@ProjectedPayload
	interface SampleDto {

		String getName();

		@DateTimeFormat(iso = ISO.DATE)
		Date getDate();

		Address getBillingAddress();

		Collection<Address> getShippingAddresses();

		interface Address {

			String getZipCode();

			String getCity();
		}
	}
}
