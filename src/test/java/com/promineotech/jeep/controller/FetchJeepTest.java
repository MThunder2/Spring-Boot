package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;



import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.promineotech.jeep.JeepSales;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;

@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = JeepSales.class)
@ActiveProfiles("test")
@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
		"classpath:flyway/migrations/V1.1__Jeep_Data.sql" }, config = @SqlConfig(encoding = "utf-8"))
class FetchJeepTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int serverPort;

	@Test
	void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {

		//  Given : valid model, trim and URI
		JeepModel model = JeepModel.WRANGLER;
		String trim = "Sport";
		String uri = String.format("http://localhost:%d/jeeps?model=%s&trim=%s", serverPort, model, trim);

		//  When : connection made to URI
		ResponseEntity<List<Jeep>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
				new ParameterizedTypeReference<>() {
				});

		//  Then : success 200 status code returned
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		
		//  And the actual list returned is same as expected list
		List<Jeep> expected = buildExpected();
		assertThat(response.getBody()).isEqualTo(expected);
	}

	protected List<Jeep> buildExpected() {
		List<Jeep> list = new LinkedList<>();
		
		//  @formatter:off
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimlevel("Sport")
				.numDoors(2)
				.wheelSize(17)
				.basePrice(new BigDecimal("28,475.00"))
				.build());
		
		list.add(Jeep.builder()
				.modelId(JeepModel.WRANGLER)
				.trimlevel("Sport")
				.numDoors(4)
				.wheelSize(17)
				.basePrice(new BigDecimal("31,975.00"))
				.build());
		//  @formatter:on
	
		return list;
		}

}
