package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.promineotech.jeep.Constants;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.service.JeepSalesService;

class FetchJeepTest {

	@Nested
	@EnableAutoConfiguration
	@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
	@ActiveProfiles("test")
	@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
			"classpath:flyway/migrations/V1.1__Jeep_Data.sql" }, config = @SqlConfig(encoding = "utf-8"))
	class TestsThatDoNotPolluteTheApplicationContext {

		@Autowired
		private TestRestTemplate restTemplate;

		@LocalServerPort
		private int serverPort;

		@Test
		void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {

			// Given : valid model, trim and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim = "Sport";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When : connection made to URI
			ResponseEntity<List<Jeep>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then : success 200 status code returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

			// And: the actual list returned is same as expected list
			List<Jeep> actual = response.getBody();
			List<Jeep> expected = buildExpected();

			assertThat(actual).isEqualTo(expected);
		}

		@Test
		void testThatAnErrorMessageIsReturnedWhenAnUnknownTrimIsSupplied() {

			// Given : valid model, trim and URI
			JeepModel model = JeepModel.WRANGLER;
			String trim = "Unknown Value";
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When : connection made to URI
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then : a not found 404 status returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

			// And: an error message is returned
			Map<String, Object> error = response.getBody();

			assertErrorMessageValid(error, HttpStatus.NOT_FOUND);
		}

		@ParameterizedTest
		@MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
		void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSupplied(String model, String trim, String reason) {

			// Given : valid model, trim and URI
			String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

			// When : connection made to URI
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
					new ParameterizedTypeReference<>() {
					});

			// Then : a not found 404 status returned
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

			// And: an error message is returned
			Map<String, Object> error = response.getBody();

			assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);
		}
	}
		static Stream<Arguments> parametersForInvalidInput() {
			//@formatter:off
			return Stream.of(
					arguments("WRANGLER", "@@##%$", "trim contains non-alpha-numeric chars"),
					arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH + 1),  "Trim length too long"),
					arguments("INVALID", "Sport", "Model is not enum value")
			//@formatter:on
			);
		}

		@Nested
		@EnableAutoConfiguration
		@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
		@ActiveProfiles("test")
		@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
				"classpath:flyway/migrations/V1.1__Jeep_Data.sql" }, config = @SqlConfig(encoding = "utf-8"))
		class TestsThatPolluteTheApplicationContext {

			@Autowired
			private TestRestTemplate restTemplate;

			@LocalServerPort
			private int serverPort;

			@MockBean
			private JeepSalesService jeepSalesService;

			@Test
			void testThatAnUnplannedErrorResultsInA500Status() {

				// Given : valid model, trim and URI
				JeepModel model = JeepModel.WRANGLER;
				String trim = "Invalid";
				String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

				doThrow(new RuntimeException("Ouch")).when(jeepSalesService).fetchJeeps(model, trim);

				// When : connection made to URI
				ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
						new ParameterizedTypeReference<>() {
						});

				// Then : internal server error 500 status returned
				assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

				// And: an error message is returned
				Map<String, Object> error = response.getBody();

				assertErrorMessageValid(error, HttpStatus.INTERNAL_SERVER_ERROR);

			}

		}

		protected void assertErrorMessageValid(Map<String, Object> error, HttpStatus status) {
		//@formatter:off
		assertThat(error)
			.containsKey("message")
			.containsEntry("status code", status.value())
			.containsEntry("uri", "/jeeps")
			.containsKey("timestamp")
			.containsEntry("reason", status.getReasonPhrase());
		//@formatter:on	
		}

		protected List<Jeep> buildExpected() {
			List<Jeep> list = new LinkedList<>();

		//  @formatter:off
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(2)
				.wheelSize(17)
				.basePrice(new BigDecimal("28475.00"))
				.build());
		
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimLevel("Sport")
				.numDoors(4)
				.wheelSize(17)
				.basePrice(new BigDecimal("31975.00"))
				.build());
		//  @formatter:on

			Collections.sort(list);
			return list;
		}

	}


