public class TestTypeScript {
	public static void main(String[] args) {
		Main.main(new String[]{
				"input=test-resources/proto/",
				"output=out/output-ts/",
				"type=TypeScript",
				"json-output=out/output-ts/"
		});
	}
}
