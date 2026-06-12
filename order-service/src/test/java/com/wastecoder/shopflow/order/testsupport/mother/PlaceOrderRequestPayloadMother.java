package com.wastecoder.shopflow.order.testsupport.mother;

public final class PlaceOrderRequestPayloadMother {

	private PlaceOrderRequestPayloadMother() {
	}

	public static String aValidPayload() {
		return """
				{
				  "customerId": "22222222-2222-2222-2222-222222222222",
				  "items": [
				    { "productId": "44444444-4444-4444-4444-444444444444", "quantity": 2, "unitPrice": 10.50 }
				  ]
				}
				""";
	}

	public static String aPayloadWithoutItems() {
		return """
				{
				  "customerId": "22222222-2222-2222-2222-222222222222",
				  "items": []
				}
				""";
	}

	public static String aPayloadWithInvalidQuantity() {
		return """
				{
				  "customerId": "22222222-2222-2222-2222-222222222222",
				  "items": [
				    { "productId": "44444444-4444-4444-4444-444444444444", "quantity": 0, "unitPrice": 10.50 }
				  ]
				}
				""";
	}
}
