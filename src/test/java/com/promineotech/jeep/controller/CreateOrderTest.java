package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import com.promineotech.jeep.JeepSales;
import com.promineotech.jeep.entity.JeepModel;
import com.promineotech.jeep.entity.Order;

@EnableAutoConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = JeepSales.class)
@ActiveProfiles("test")
@Sql(scripts = { "classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
		"classpath:flyway/migrations/V1.1__Jeep_Data.sql" }, config = @SqlConfig(encoding = "utf-8"))
class CreateOrderTest  {
	
	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int serverPort;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void testCreateOrderReturnsSuccess201() {
		// Given: order as JSON
		String body = createOrderBody();
		String uri = String.format("http://localhost:%d/orders", serverPort); 
		
		int numRowsOrders = JdbcTestUtils.countRowsInTable(jdbcTemplate, "orders");
		int numRowsOptions = JdbcTestUtils.countRowsInTable(jdbcTemplate, "order_options");
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		HttpEntity<String> bodyEntity = new HttpEntity<>(body, headers);
		
		// When: order is sent
		ResponseEntity<Order> response = restTemplate.exchange(uri,
			    HttpMethod.POST, bodyEntity, Order.class);
		
		// Then: 201 status returned
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
			
		// And: returned order is correct
		assertThat(response.getBody()).isNotNull();
		
		Order order = response.getBody();
		assertThat(order.getCustomer().getCustomerId()).isEqualTo("OTXOA_GISBERT");
		assertThat(order.getModel().getModelId()).isEqualTo(JeepModel.GLADIATOR);
		assertThat(order.getModel().getTrimLevel()).isEqualTo("80th Anniversary");
		assertThat(order.getModel().getNumDoors()).isEqualTo(4);
		assertThat(order.getColor().getColorId()).isEqualTo("EXT_BIKINI_METALLIC");
		assertThat(order.getEngine().getEngineId()).isEqualTo("2_0_TURBO");
		assertThat(order.getTire().getTireId()).isEqualTo("37_YOKOHAMA");
		assertThat(order.getOptions()).hasSize(6);

		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "orders")).isEqualTo(numRowsOrders + 1);
		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "order_options")).isEqualTo(numRowsOptions + 6);
	}

	protected String createOrderBody() {
		// @formatter: off
		return  "{\n"
				+ "  \"customer\":\"OTXOA_GISBERT\",\n"
				+ "  \"model\":\"GLADIATOR\",\n"
				+ "  \"trim\":\"80th Anniversary\",\n"
				+ "  \"doors\":4,\n"
				+ "  \"color\":\"EXT_BIKINI_METALLIC\",\n"
				+ "  \"engine\":\"2_0_TURBO\",\n"
				+ "  \"tire\":\"37_YOKOHAMA\",\n"
				+ "  \"options\":[\n"
				+ "    \"DOOR_MOPAR_REINFORCE\",\n"
				+ "    \"EXT_MOPAR_STEP_BLACK\",\n"
				+ "    \"EXT_WARN_WINCH\",\n"
				+ "    \"EXT_WARN_BUMPER_FRONT\",\n"
				+ "    \"EXT_WARN_BUMPER_REAR\",\n"
				+ "    \"EXT_MOPAR_CAMERA\"\n"
				+ "  ]\n"
				+ "}";
		 // @formatter: on		
	}

}
