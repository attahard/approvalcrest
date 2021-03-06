package com.github.karsaig.approvalcrest;

import static com.github.karsaig.approvalcrest.MatcherAssert.assertThat;
import static com.github.karsaig.approvalcrest.matcher.Matchers.sameJsonAsApproved;
import static com.github.karsaig.approvalcrest.testdata.cyclic.CircularReferenceBean.Builder.circularReferenceBean;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import com.github.karsaig.approvalcrest.matcher.GsonConfiguration;
import com.github.karsaig.approvalcrest.testdata.ClosableFields;
import com.github.karsaig.approvalcrest.testdata.IterableFields;
import com.github.karsaig.approvalcrest.testdata.cyclic.CircularReferenceBean;
import com.github.karsaig.approvalcrest.testdata.cyclic.Element;
import com.github.karsaig.approvalcrest.testdata.cyclic.Four;
import com.github.karsaig.approvalcrest.testdata.cyclic.One;
import com.github.karsaig.approvalcrest.testdata.cyclic.Two;
import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JsonMatcherCircularReferenceTest {

	@Test
	public void doesNothingWhenAutoDetectCircularReferenceIsCalled() {
		CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

		assertThat(actual, sameJsonAsApproved());
	}

	@Test
	public void shouldNotThrowStackOverFlowExceptionWhenExpectedBeanIsNullAndTheActualNotNull() {
		CircularReferenceBean actual = circularReferenceBean("parent", "child1", "child2").build();

		assertThrows(AssertionFailedError.class, () -> {
			assertThat(actual, sameJsonAsApproved());
		});
	}

	@Test
	public void shouldNotThrowStackOverflowExceptionWhenCircularReferenceExistsInAComplexGraph() {
		Four root = new Four();
		Four child1 = new Four();
		Four child2 = new Four();
		root.setGenericObject(child1);
		child1.setGenericObject(root); // circular
		root.setSubClassField(child2);

		One subRoot = new One();
		One subRootChild = new One();
		subRoot.setGenericObject(subRootChild);
		subRootChild.setGenericObject(subRoot); // circular

		child2.setGenericObject(subRoot);

		assertThat(root, sameJsonAsApproved());
	}

	@Test
	public void doesNotThrowStackOverflowErrorWhenComparedObjectsHaveDifferentCircularReferences() {
		Object actual = new One();
		One actualChild = new One();
		((One)actual).setGenericObject(actualChild);
		actualChild.setGenericObject(actual);

		assertThrows(AssertionFailedError.class, () -> {
			assertThat(actual, sameJsonAsApproved());
		});
	}

	@Test
	public void shouldNotTakeAges() {
		assertTimeout(Duration.ofMillis(150), () -> {
			assertThrows(AssertionFailedError.class, () -> {
				assertThat(Element.TWO, sameJsonAsApproved());
			});
			return null;
		});
	}

	@Test
	public void doesNotThrowStackOverflowErrorWhenCircularReferenceIsInTheSecondLevelUpperClass() {
		assertThat(new RuntimeException(), sameJsonAsApproved());
	}

	@Test
	public void doesNotThrowStackOverflowExceptionWithAMoreNestedObject() {
		Throwable throwable = new Throwable(new Exception(new RuntimeException(new ClassCastException())));

		assertThat(throwable, sameJsonAsApproved());
	}

	@Test
	public void doesNotReturn0x1InDiagnosticWhenUnnecessary() {
		try {
			assertThat(Element.ONE, sameJsonAsApproved());

			fail("expected AssertionFailedError");
		} catch (AssertionFailedError e) {
			assertThat(e.getExpected().toString(), not(containsString("0x1")));
			assertThat(e.getActual().toString(), not(containsString("0x1")));
		}
	}

	@Test
	public void doesNotFailWithClosableFields() {
		ClosableFields input = new ClosableFields();
		input.setInput(new ByteArrayInputStream("DummyInput".getBytes()));
		input.setOutput(new ByteArrayOutputStream());

		assertThat(input, sameJsonAsApproved());
	}

	@Test
	public void doesNotFailWithIterableFields() {
		SQLException sqlException = new SQLException("dummy reason");
		IterableFields input = new IterableFields();
		Two dummy1 = new Two();
		dummy1.setGenericObject("Dummy1");
		Two dummy2 = new Two();
		dummy2.setGenericObject("Dummy1");
		input.setTwos(Sets.newHashSet(dummy1,dummy2));
		input.setOnes(sqlException);

		assertThat(input, sameJsonAsApproved());
	}

	@Test
	public void shouldNotThrowStackOverflowExceptionWhenCircularReferenceExistsIsSkippedButCustomSerialized() {
		Four root = new Four();
		Four child1 = new Four();
		Four child2 = new Four();
		root.setGenericObject(child1);
		root.setSubClassField(child2);

		One subRoot = new One();
		One subRootChild = new One();
		subRoot.setGenericObject(subRootChild);
		subRootChild.setGenericObject(subRoot); // circular
		Function<Object, Boolean> skipper1 = input -> One.class.isInstance(input);
		GsonConfiguration config = new GsonConfiguration();
		config.addTypeAdapter(One.class, new DummyOneJsonSerializer());

		child2.setGenericObject(subRoot);

		assertThat(root, sameJsonAsApproved().skipCircularReferenceCheck(skipper1).withGsonConfiguration(config));
	}

	private class DummyOneJsonSerializer implements JsonDeserializer<One>,JsonSerializer<One>  {

		@Override
		public One deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
			return null;
		}

		@Override
		public JsonElement serialize(final One src, final Type typeOfSrc, final JsonSerializationContext context) {
			return new JsonPrimitive("customSerializedOneCircle");
		}

	}
}
