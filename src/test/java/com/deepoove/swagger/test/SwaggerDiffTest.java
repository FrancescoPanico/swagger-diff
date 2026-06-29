package com.deepoove.swagger.test;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;
import com.deepoove.swagger.diff.output.HtmlRender;
import com.deepoove.swagger.diff.output.JsonRender;
import com.deepoove.swagger.diff.output.MarkdownRender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


public class SwaggerDiffTest {

	final String SWAGGER_V2_DOC1 = "petstore_v2_1.json";
	final String SWAGGER_V2_DOC2 = "petstore_v2_2.json";
	final String SWAGGER_V2_EMPTY_DOC = "petstore_v2_empty.json";
	final String SWAGGER_V2_HTTP = "http://petstore.swagger.io/v2/swagger.json";

	final String SWAGGER_V3_EMPTY_DOC = "petstore_v3_empty.json";

	@Test
	public void testEqualV3() {
		SwaggerDiff diff = SwaggerDiff.compareV3(SWAGGER_V3_EMPTY_DOC, SWAGGER_V3_EMPTY_DOC);
		assertEqual(diff);
	}

	@Test
	public void testEqual() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC2, SWAGGER_V2_DOC2);
		assertEqual(diff);
	}

	@Test
	public void testNewApi() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_EMPTY_DOC, SWAGGER_V2_DOC2);
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"http://deepoove.com/swagger-diff/stylesheets/demo.css")
						.render(diff);

		try {
			FileWriter fw = new FileWriter(
					"testNewApi.html");
			fw.write(html);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(newEndpoints.size() > 0);
		Assert.assertTrue(missingEndpoints.isEmpty());
		Assert.assertTrue(changedEndPoints.isEmpty());

	}

	@Test
	public void testDeprecatedApi() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_EMPTY_DOC);
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"http://deepoove.com/swagger-diff/stylesheets/demo.css")
						.render(diff);

		try {
			FileWriter fw = new FileWriter(
					"testDeprecatedApi.html");
			fw.write(html);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertTrue(newEndpoints.isEmpty());
		Assert.assertTrue(missingEndpoints.size() > 0);
		Assert.assertTrue(changedEndPoints.isEmpty());

	}
	
	@Test
	public void testDiff() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		String html = new HtmlRender("Changelog",
				"http://deepoove.com/swagger-diff/stylesheets/demo.css")
				.render(diff);

		try {
			FileWriter fw = new FileWriter(
					"testDiff.html");
			fw.write(html);
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		Assert.assertFalse(changedEndPoints.isEmpty());
		
	}
	
	@Test
	public void testDiffAndMarkdown() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		String render = new MarkdownRender().render(diff);
		try {
			FileWriter fw = new FileWriter(
					"testDiff.md");
			fw.write(render);
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

    @Test
    public void testEqualRaw() throws IOException {
        String rawJson = load(SWAGGER_V2_DOC2);

        SwaggerDiff diff = SwaggerDiff.compareV2Raw(rawJson, rawJson);
        assertEqual(diff);
    }


    @Test
    public void testNewApiRaw() throws IOException {
        SwaggerDiff diff = SwaggerDiff.compareV2Raw(load(SWAGGER_V2_EMPTY_DOC), load(SWAGGER_V2_DOC2));

        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();

        Assert.assertTrue(newEndpoints.size() > 0);
        Assert.assertTrue(missingEndpoints.isEmpty());
        Assert.assertTrue(changedEndPoints.isEmpty());

    }

    @Test
    public void testDeprecatedApiRaw() throws IOException {
        SwaggerDiff diff = SwaggerDiff.compareV2Raw(load(SWAGGER_V2_DOC1), load(SWAGGER_V2_EMPTY_DOC));
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();

        Assert.assertTrue(newEndpoints.isEmpty());
        Assert.assertTrue(missingEndpoints.size() > 0);
        Assert.assertTrue(changedEndPoints.isEmpty());
    }

	@Test
	public void testEqualJson() {
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream(SWAGGER_V2_DOC1);
			JsonNode json = new ObjectMapper().readTree(inputStream);
			SwaggerDiff diff = SwaggerDiff.compareRaw(json.toString(), json.toString());
			assertEqual(diff);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

    private String load(String location) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(location)) {
            if (is == null) return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                 BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

	@Test
	public void testJsonRender() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		String render = new JsonRender().render(diff);
		try {
			FileWriter fw = new FileWriter(
					"testDiff.json");
			fw.write(render);
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testInputBodyArray() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		Map<String, ChangedEndpoint> changedEndpointMap = diff.getChangedEndpoints().stream().collect(Collectors.toMap(ChangedEndpoint::getPathUrl, e -> e));
		Lists.newArrayList("/user/createWithArray", "/user/createWithList").forEach(name -> {
			Assert.assertTrue("Expecting changed endpoint " + name, changedEndpointMap.containsKey(name));
			ChangedEndpoint endpoint = changedEndpointMap.get(name);
			Assert.assertEquals(1, endpoint.getChangedOperations().size());
			Assert.assertTrue("Expecting POST method change", endpoint.getChangedOperations().containsKey(HttpMethod.POST));
			Assert.assertEquals(0, endpoint.getChangedOperations().get(HttpMethod.POST).getMissingParameters().size());
			Assert.assertEquals(0, endpoint.getChangedOperations().get(HttpMethod.POST).getAddParameters().size());
			Assert.assertEquals(0, endpoint.getChangedOperations().get(io.swagger.v3.oas.models.PathItem.HttpMethod.POST).getChangedParameter().size());

			// assert changed property counts
			ChangedOperation changedOp = endpoint.getChangedOperations().get(io.swagger.v3.oas.models.PathItem.HttpMethod.POST);
			Assert.assertEquals(3, changedOp.getAddRequestProps().size());
			Assert.assertEquals(3, changedOp.getMissingRequestProps().size());
			Assert.assertEquals(0, changedOp.getChangedRequestProps().size());

			// assert embedded array change is one of the missing properties
			List<ElProperty> missingProperties = changedOp.getMissingRequestProps();
			Set<String> elementPaths = missingProperties.stream().map(ElProperty::getEl).collect(Collectors.toSet());
			Assert.assertTrue(elementPaths.contains("favorite.tags.removedField") || elementPaths.contains("body.favorite.tags.removedField"));
		});
	}

	@Test
	public void testResponseBodyArray() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		Map<String, ChangedEndpoint> changedEndpointMap = diff.getChangedEndpoints().stream().collect(Collectors.toMap(ChangedEndpoint::getPathUrl, e -> e));
		Lists.newArrayList("/pet/findByStatus", "/pet/findByTags").forEach(name -> {
			Assert.assertTrue("Expecting changed endpoint " + name, changedEndpointMap.containsKey(name));
			ChangedEndpoint endpoint = changedEndpointMap.get(name);
			Assert.assertEquals(1, endpoint.getChangedOperations().size());
			Assert.assertTrue("Expecting GET method change", endpoint.getChangedOperations().containsKey(HttpMethod.GET));

			// assert changed property counts
			ChangedOperation changedOutput = endpoint.getChangedOperations().get(HttpMethod.GET);
			Assert.assertEquals(3, changedOutput.getAddProps().size());
			Assert.assertEquals(3, changedOutput.getMissingProps().size());
			Assert.assertEquals(0, changedOutput.getChangedProps().size());

			// assert embedded array change is one of the missing properties
			List<ElProperty> missingProperties =changedOutput.getMissingProps();
			Set<String> elementPaths = missingProperties.stream().map(ElProperty::getEl).collect(Collectors.toSet());
			Assert.assertTrue(elementPaths.contains("tags.removedField"));
		});
	}

	@Test
	public void testDetectProducesAndConsumes() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		Map<String, ChangedEndpoint> changedEndpointMap = diff.getChangedEndpoints().stream().collect(Collectors.toMap(ChangedEndpoint::getPathUrl, e -> e));
		Assert.assertTrue("Expecting changed endpoint " + "/store/order", changedEndpointMap.containsKey("/store/order"));
		ChangedEndpoint endpoint = changedEndpointMap.get("/store/order");
		Assert.assertTrue("Expecting POST method change", endpoint.getChangedOperations().containsKey(HttpMethod.POST));
		ChangedOperation changedOperation = endpoint.getChangedOperations().get(HttpMethod.POST);
		Assert.assertEquals(1, changedOperation.getAddConsumes().size());
		Assert.assertEquals(1, changedOperation.getMissingConsumes().size());
		Assert.assertEquals(0, changedOperation.getAddProduces().size());
		Assert.assertEquals(1, changedOperation.getMissingProduces().size());
	}

	@Test
	public void testChangedPropertyMetadata() {
		SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V2_DOC1, SWAGGER_V2_DOC2);
		Map<String, ChangedEndpoint> changedEndpointMap = diff.getChangedEndpoints().stream().collect(Collectors.toMap(ChangedEndpoint::getPathUrl, e -> e));
		String postOrder = "/store/order";
		String getOrder = "/store/order/{orderId}";

		Assert.assertTrue("Expecting changed endpoint " + postOrder, changedEndpointMap.containsKey(postOrder));
		ChangedEndpoint postOrderChg = changedEndpointMap.get(postOrder);
		ChangedOperation postOrderChgOp = postOrderChg.getChangedOperations().get(HttpMethod.POST);
		List<ElProperty> postChgProps = postOrderChgOp.getChangedRequestProps();
		Assert.assertEquals(2, postChgProps.size());
		ElProperty orderIdProp = postChgProps.stream().filter(cp -> {
			return cp.getEl().equalsIgnoreCase("body.id") || cp.getEl().equalsIgnoreCase("id");}).findFirst().get();
		Assert.assertTrue(orderIdProp.isTypeChange());
		Assert.assertFalse(orderIdProp.isNewEnums());
		Assert.assertFalse(orderIdProp.isRemovedEnums());
		ElProperty statusProp = postChgProps.stream().filter(cp -> {
			return cp.getEl().equalsIgnoreCase("body.status") || cp.getEl().equalsIgnoreCase("status");}).findFirst().get();
		Assert.assertFalse(statusProp.isTypeChange());
		Assert.assertTrue(statusProp.isNewEnums());
		Assert.assertTrue(statusProp.isRemovedEnums());


		Assert.assertTrue("Expecting changed endpoint " + getOrder, changedEndpointMap.containsKey(getOrder));
		ChangedEndpoint getOrderChg = changedEndpointMap.get(getOrder);
		ChangedOperation getOrderChgOp = getOrderChg.getChangedOperations().get(HttpMethod.GET);
		List<ElProperty> getChgProps = getOrderChgOp.getChangedProps();
		Assert.assertEquals(2, getChgProps.size());

		orderIdProp = getChgProps.stream().filter(cp -> {
			return cp.getEl().equalsIgnoreCase("id");}).findFirst().get();
		Assert.assertTrue(orderIdProp.isTypeChange());
		Assert.assertFalse(orderIdProp.isNewEnums());
		Assert.assertFalse(orderIdProp.isRemovedEnums());
		statusProp = getChgProps.stream().filter(cp -> {
			return cp.getEl().equalsIgnoreCase("status");}).findFirst().get();
		Assert.assertFalse(statusProp.isTypeChange());
		Assert.assertTrue(statusProp.isNewEnums());
		Assert.assertTrue(statusProp.isRemovedEnums());
	}

	private void assertEqual(SwaggerDiff diff) {
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
		Assert.assertTrue(newEndpoints.isEmpty());
		Assert.assertTrue(missingEndpoints.isEmpty());
		Assert.assertTrue(changedEndPoints.isEmpty());

	}
	@Test
	public void x() {
		SwaggerDiff diff = SwaggerDiff.compareV3(
			"src/test/resources/petstore_v3_diff1.json",
			"src/test/resources/petstore_v3_diff2.json"
		);

		List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
		for(int i=0;i<changedEndpoints.size();i++){
			ChangedEndpoint ce = changedEndpoints.get(i);
			System.out.println(ce.getPathUrl());
			System.out.println(ce.isDiff());
			for (HttpMethod keys : ce.getChangedOperations().keySet())
				{
					System.out.println(keys);
					ChangedOperation changedOperation = ce.getChangedOperations().get(keys);
					System.out.println(changedOperation.isDiff());
					System.out.println(changedOperation.getChangedProps().stream().map(ElProperty::getEl).collect(Collectors.joining(",")));
				}
		}
			

		// expect one changed endpoint "/item"
		Assert.assertEquals(1, changedEndpoints.size());

		ChangedEndpoint ce = changedEndpoints.get(0);
		//Test chenges are in correct path
		Assert.assertEquals("/item", ce.getPathUrl());
		// POST: request parameters changed (age added), responses changed (201 added)
		// GET: added, responses changed (200 added)
		Assert.assertEquals(1,ce.getChangedOperations().size());//One operation is changed: add age parameter
		Assert.assertEquals(HttpMethod.POST,ce.getChangedOperations().keySet().toArray()[0]);
		Assert.assertEquals(HttpMethod.GET,ce.getNewOperations().keySet().toArray()[0]);



		// POST: 
		// a new request property "age" is added
		ChangedOperation changedPostOp =
			ce.getChangedOperations().get(HttpMethod.POST);

		Assert.assertEquals(1, changedPostOp.getAddRequestProps().size());

		Optional<ElProperty> age = changedPostOp.getAddRequestProps().stream()
			.filter(p -> "age".equals(p.getEl())).findFirst();

		Assert.assertTrue("age property should be added", age.isPresent());
		Assert.assertTrue("age property should be added", age.get().getProperty().getDescription().equals("age of item"));
		Assert.assertTrue("age property should be added", age.get().getProperty().getType().equals("integer"));
		
		// an old property oldName is removed
		Assert.assertEquals(1, changedPostOp.getMissingRequestProps().size());
		Optional<ElProperty> oldName = changedPostOp.getMissingRequestProps().stream()
			.filter(p -> "oldName".equals(p.getEl())).findFirst();
		Assert.assertTrue("oldName property should be removed", oldName.isPresent());	
		Assert.assertTrue("oldName property should be removed as deprecated", oldName.get().getProperty().getDeprecated());	
		Assert.assertTrue("oldName property should be removed as type integer", oldName.get().getProperty().getType().equals("string"));	


		// GET: 
		// a new query parameter "type" is added
		Operation getOp =
			ce.getNewOperations().get(HttpMethod.GET);

		Assert.assertEquals(2, getOp.getParameters().size());

		//check type parameter 
		Optional<Parameter> parameter = getOp.getParameters().stream()
			.filter(p -> "type".equals(p.getName())).findFirst();
		
		Assert.assertTrue("type query parameter should be added", parameter.isPresent());
		Assert.assertTrue("type query parameter should be added", parameter.get().getName().equals("type"));
		Assert.assertTrue("type query parameter should be added", parameter.get().getIn().equals("query"));
		Assert.assertTrue("type query parameter should be added", parameter.get().getSchema().getType().equals("string"));
		Assert.assertTrue("type query parameter should be added", parameter.get().getSchema().getDescription().equals("the type of the item"));
		
		//check size parameter 
		Optional<Parameter> parameter2 = getOp.getParameters().stream()
			.filter(p -> "size".equals(p.getName())).findFirst();
		
		Assert.assertTrue("size query parameter should be added", parameter2.isPresent());
		Assert.assertTrue("size query parameter should be added", parameter2.get().getName().equals("size"));
		Assert.assertTrue("size query parameter should be added", parameter2.get().getIn().equals("query"));
		Assert.assertTrue("size query parameter should be added", parameter2.get().getSchema().getType().equals("integer"));
		Assert.assertTrue("size query parameter should be added", parameter2.get().getDescription().equals("the size of the item"));
		Assert.assertTrue("size query parameter should be added", parameter2.get().getRequired());
		
		
		
	}

	@Test
	public void testResponseStatusDiff() {
		SwaggerDiff diff = SwaggerDiff.compareV3(
			"src/test/resources/petstore_v3_diff1.json",
			"src/test/resources/petstore_v3_diff2.json"
		);

		List<ChangedEndpoint> changed = diff.getChangedEndpoints();
		Assert.assertFalse("Atteso almeno un endpoint cambiato", changed.isEmpty());

		ChangedOperation op = changed.get(0).getChangedOperations()
				.values().iterator().next();

		// 201 aggiunto
		Assert.assertTrue("Atteso status 201 aggiunto",
				op.getAddResponses().containsKey("201"));
		// 404 rimosso
		Assert.assertTrue("Atteso status 404 rimosso",
				op.getMissingResponses().containsKey("404"));
		// 200 con schema cambiato (se hai modificato lo schema del 200)
		boolean has200Changed = op.getChangedResponses().stream()
				.anyMatch(cr -> "200".equals(cr.getStatusCode()) && cr.isDiff());
		Assert.assertTrue("Atteso schema del 200 cambiato", has200Changed);
	}

	@Test
	public void testResponseFullDiff() {
		SwaggerDiff diff = SwaggerDiff.compareV3(
			"src/test/resources/petstore_v3_diff1.json",
			"src/test/resources/petstore_v3_diff2.json"
		);

		ChangedOperation op = diff.getChangedEndpoints().get(0)
				.getChangedOperations().values().iterator().next();

		ChangedResponse r200 = op.getChangedResponses().stream()
				.filter(cr -> "200".equals(cr.getStatusCode()))
				.findFirst().orElse(null);

		Assert.assertNotNull(r200);

		// description
		Assert.assertTrue(r200.isDescriptionChanged());
		Assert.assertEquals("ok", r200.getOldDescription());
		Assert.assertEquals("update Success", r200.getNewDescription());
	}
		
		

}
