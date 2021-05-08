/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.LongCodec;
import org.bson.codecs.MapCodec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.entities.AsymmetricalCreatorModel;
import org.bson.codecs.pojo.entities.AsymmetricalIgnoreModel;
import org.bson.codecs.pojo.entities.AsymmetricalModel;
import org.bson.codecs.pojo.entities.ConcreteCollectionsModel;
import org.bson.codecs.pojo.entities.ConstructorNotPublicModel;
import org.bson.codecs.pojo.entities.ConventionModel;
import org.bson.codecs.pojo.entities.ConverterModel;
import org.bson.codecs.pojo.entities.CustomPropertyCodecOptionalModel;
import org.bson.codecs.pojo.entities.GenericTreeModel;
import org.bson.codecs.pojo.entities.InvalidCollectionModel;
import org.bson.codecs.pojo.entities.InvalidGetterAndSetterModel;
import org.bson.codecs.pojo.entities.InvalidMapModel;
import org.bson.codecs.pojo.entities.InvalidMapPropertyCodecProvider;
import org.bson.codecs.pojo.entities.InvalidSetterArgsModel;
import org.bson.codecs.pojo.entities.MapStringObjectModel;
import org.bson.codecs.pojo.entities.Optional;
import org.bson.codecs.pojo.entities.OptionalPropertyCodecProvider;
import org.bson.codecs.pojo.entities.PrimitivesModel;
import org.bson.codecs.pojo.entities.PrivateSetterFieldModel;
import org.bson.codecs.pojo.entities.SimpleEnum;
import org.bson.codecs.pojo.entities.SimpleEnumModel;
import org.bson.codecs.pojo.entities.SimpleModel;
import org.bson.codecs.pojo.entities.SimpleNestedPojoModel;
import org.bson.codecs.pojo.entities.UpperBoundsModel;
import org.bson.codecs.pojo.entities.conventions.AnnotationModel;
import org.bson.codecs.pojo.entities.conventions.CollectionsGetterImmutableModel;
import org.bson.codecs.pojo.entities.conventions.CollectionsGetterMutableModel;
import org.bson.codecs.pojo.entities.conventions.CollectionsGetterNonEmptyModel;
import org.bson.codecs.pojo.entities.conventions.CollectionsGetterNullModel;
import org.bson.codecs.pojo.entities.conventions.CreatorConstructorPrimitivesModel;
import org.bson.codecs.pojo.entities.conventions.CreatorConstructorThrowsExceptionModel;
import org.bson.codecs.pojo.entities.conventions.CreatorMethodThrowsExceptionModel;
import org.bson.codecs.pojo.entities.conventions.MapGetterImmutableModel;
import org.bson.codecs.pojo.entities.conventions.MapGetterMutableModel;
import org.bson.codecs.pojo.entities.conventions.MapGetterNonEmptyModel;
import org.bson.codecs.pojo.entities.conventions.MapGetterNullModel;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.Conventions.DEFAULT_CONVENTIONS;
import static org.bson.codecs.pojo.Conventions.NO_CONVENTIONS;
import static org.bson.codecs.pojo.Conventions.SET_PRIVATE_FIELDS_CONVENTION;
import static org.bson.codecs.pojo.Conventions.USE_GETTERS_FOR_SETTERS;
import static org.junit.Assert.assertTrue;

public final class PojoCustomTest extends PojoTestCase {

    @Test
    public void testRegisterClassModelPreferredOverClass() {
        ClassModel<SimpleModel> classModel = ClassModel.builder(SimpleModel.class).enableDiscriminator(true).build();
        PojoCodecProvider.Builder builder = PojoCodecProvider.builder().automatic(true).register(SimpleModel.class).register(classModel);

        roundTrip(builder, getSimpleModel(), "{_t: 'org.bson.codecs.pojo.entities.SimpleModel', 'integerField': 42,"
                + "'stringField': 'myString'}");
    }

    @Test
    public void testPackageDiscriminator() {
        AnnotationModel model = new AnnotationModel("myId", new AnnotationModel("child", null, null),
                new AnnotationModel("alternative", null, null));

        roundTrip(PojoCodecProvider.builder().register("org.bson.codecs.pojo.entities", "org.bson.codecs.pojo.entities.conventions"), model,
                "{_id: 'myId', _cls: 'MyAnnotationModel', renamed: {_id: 'alternative'}, child: {_id: 'child'}}");
    }

    @Test
    public void testAsymmetricalModel() {
        AsymmetricalModel model = new AsymmetricalModel(42);

        encodesTo(getPojoCodecProviderBuilder(AsymmetricalModel.class), model, "{foo: 42}");
        decodesTo(getPojoCodecProviderBuilder(AsymmetricalModel.class), "{bar: 42}", model);
    }

    @Test
    public void testAsymmetricalCreatorModel() {
        AsymmetricalCreatorModel model = new AsymmetricalCreatorModel("Foo", "Bar");

        encodesTo(getPojoCodecProviderBuilder(AsymmetricalCreatorModel.class), model, "{baz: 'FooBar'}");
        decodesTo(getPojoCodecProviderBuilder(AsymmetricalCreatorModel.class), "{a: 'Foo', b: 'Bar'}", model);
    }

    @Test
    public void testAsymmetricalIgnoreModel() {
        AsymmetricalIgnoreModel encode = new AsymmetricalIgnoreModel("property", "getter", "setter", "getterAndSetter");
        AsymmetricalIgnoreModel decoded = new AsymmetricalIgnoreModel();
        decoded.setGetterIgnored("getter");

        encodesTo(getPojoCodecProviderBuilder(AsymmetricalIgnoreModel.class), encode, "{'setterIgnored': 'setter'}");
        decodesTo(getPojoCodecProviderBuilder(AsymmetricalIgnoreModel.class),
                "{'propertyIgnored': 'property', 'getterIgnored': 'getter', 'setterIgnored': 'setter', "
                        + "'getterAndSetterIgnored': 'getterAndSetter'}", decoded);
    }

    @Test
    public void testConventionsEmpty() {
        ClassModelBuilder<ConventionModel> classModel = ClassModel.builder(ConventionModel.class).conventions(NO_CONVENTIONS);
        ClassModelBuilder<SimpleModel> nestedClassModel = ClassModel.builder(SimpleModel.class).conventions(NO_CONVENTIONS);

        roundTrip(getPojoCodecProviderBuilder(classModel, nestedClassModel), getConventionModel(),
                "{'myFinalField': 10, 'myIntField': 10, 'customId': 'id',"
                        + "'child': {'myFinalField': 10, 'myIntField': 10, 'customId': 'child',"
                        + "          'simpleModel': {'integerField': 42, 'stringField': 'myString' } } }");
    }

    @Test
    public void testConventionsCustom() {
        List<Convention> conventions = Collections.<Convention>singletonList(
                new Convention() {
                    @Override
                    public void apply(final ClassModelBuilder<?> classModelBuilder) {
                        for (PropertyModelBuilder<?> fieldModelBuilder : classModelBuilder.getPropertyModelBuilders()) {
                            fieldModelBuilder.discriminatorEnabled(false);
                            fieldModelBuilder.readName(
                                    fieldModelBuilder.getName()
                                            .replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                            fieldModelBuilder.writeName(
                                    fieldModelBuilder.getName()
                                            .replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                        }
                        if (classModelBuilder.getProperty("customId") != null) {
                            classModelBuilder.idPropertyName("customId");
                        }
                        classModelBuilder.enableDiscriminator(true);
                        classModelBuilder.discriminatorKey("_cls");
                        classModelBuilder.discriminator(classModelBuilder.getType().getSimpleName()
                                .replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                    }
                });

        ClassModelBuilder<ConventionModel> classModel = ClassModel.builder(ConventionModel.class).conventions(conventions);
        ClassModelBuilder<SimpleModel> nestedClassModel = ClassModel.builder(SimpleModel.class).conventions(conventions);

        roundTrip(getPojoCodecProviderBuilder(classModel, nestedClassModel), getConventionModel(),
                "{ '_id': 'id', '_cls': 'convention_model', 'my_final_field': 10, 'my_int_field': 10,"
                        + "'child': { '_id': 'child', 'my_final_field': 10, 'my_int_field': 10, "
                        + "           'simple_model': {'integer_field': 42, 'string_field': 'myString' } } }");
    }

    @Test
    public void testSetPrivateFieldConvention() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(PrivateSetterFieldModel.class);
        ArrayList<Convention> conventions = new ArrayList<Convention>(DEFAULT_CONVENTIONS);
        conventions.add(SET_PRIVATE_FIELDS_CONVENTION);
        builder.conventions(conventions);

        roundTrip(builder, new PrivateSetterFieldModel(1, "2", asList("a", "b")),
                "{'someMethod': 'some method', 'integerField': 1, 'stringField': '2', listField: ['a', 'b']}");
    }

    @Test
    public void testUseGettersForSettersConvention() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(CollectionsGetterMutableModel.class, MapGetterMutableModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new CollectionsGetterMutableModel(asList(1, 2)), "{listField: [1, 2]}");
        roundTrip(builder, new MapGetterMutableModel(Collections.singletonMap("a", 3)), "{mapField: {a: 3}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionInvalidTypeForCollection() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(CollectionsGetterMutableModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        decodingShouldFail(getCodec(builder, CollectionsGetterMutableModel.class), "{listField: ['1', '2']}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionInvalidTypeForMap() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(MapGetterMutableModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        decodingShouldFail(getCodec(builder, MapGetterMutableModel.class), "{mapField: {a: '1'}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionImmutableCollection() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(CollectionsGetterImmutableModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new CollectionsGetterImmutableModel(asList(1, 2)), "{listField: [1, 2]}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionImmutableMap() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(MapGetterImmutableModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new MapGetterImmutableModel(Collections.singletonMap("a", 3)), "{mapField: {a: 3}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionNullCollection() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(CollectionsGetterNullModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new CollectionsGetterNullModel(asList(1, 2)), "{listField: [1, 2]}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionNullMap() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(MapGetterNullModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new MapGetterNullModel(Collections.singletonMap("a", 3)), "{mapField: {a: 3}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionNotEmptyCollection() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(CollectionsGetterNonEmptyModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new CollectionsGetterNonEmptyModel(asList(1, 2)), "{listField: [1, 2]}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testUseGettersForSettersConventionNotEmptyMap() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(MapGetterNonEmptyModel.class)
                .conventions(getDefaultAndUseGettersConvention());

        roundTrip(builder, new MapGetterNonEmptyModel(Collections.singletonMap("a", 3)), "{mapField: {a: 3}}");
    }

    @Test
    public void testEnumSupportWithCustomCodec() {
        CodecRegistry registry = fromRegistries(getCodecRegistry(getPojoCodecProviderBuilder(SimpleEnumModel.class)),
                fromCodecs(new SimpleEnumCodec()));
        roundTrip(registry, new SimpleEnumModel(SimpleEnum.BRAVO), "{ 'myEnum': 1 }");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomCodec() {
        ObjectId id = new ObjectId();
        ConverterModel model = new ConverterModel(id.toHexString(), "myName");

        ClassModelBuilder<ConverterModel> classModel = ClassModel.builder(ConverterModel.class);
        PropertyModelBuilder<String> idPropertyModelBuilder = (PropertyModelBuilder<String>) classModel.getProperty("id");
        idPropertyModelBuilder.codec(new StringToObjectIdCodec());

        roundTrip(getPojoCodecProviderBuilder(classModel), model,
                format("{'_id': {'$oid': '%s'}, 'name': 'myName'}", id.toHexString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomPropertySerializer() {
        SimpleModel model = getSimpleModel();
        model.setIntegerField(null);
        ClassModelBuilder<SimpleModel> classModel = ClassModel.builder(SimpleModel.class);
        ((PropertyModelBuilder<Integer>) classModel.getProperty("integerField"))
                .propertySerialization(new PropertySerialization<Integer>() {
                    @Override
                    public boolean shouldSerialize(final Integer value) {
                        return true;
                    }
                });

        roundTrip(getPojoCodecProviderBuilder(classModel), model, "{'integerField': null, 'stringField': 'myString'}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanHandleNullValuesForNestedModels() {
        SimpleNestedPojoModel model = getSimpleNestedPojoModel();
        model.setSimple(null);
        ClassModelBuilder<SimpleNestedPojoModel> classModel = ClassModel.builder(SimpleNestedPojoModel.class);
        ((PropertyModelBuilder<SimpleModel>) classModel.getProperty("simple"))
                .propertySerialization(new PropertySerialization<SimpleModel>() {
                    @Override
                    public boolean shouldSerialize(final SimpleModel value) {
                        return true;
                    }
                });
        ClassModelBuilder<SimpleModel> classModelSimple = ClassModel.builder(SimpleModel.class);

        roundTrip(getPojoCodecProviderBuilder(classModel, classModelSimple), model, "{'simple': null}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanHandleNullValuesForCollectionsAndMaps() {
        ConcreteCollectionsModel model = getConcreteCollectionsModel();
        model.setCollection(null);
        model.setMap(null);

        ClassModelBuilder<ConcreteCollectionsModel> classModel =
                ClassModel.builder(ConcreteCollectionsModel.class);
        ((PropertyModelBuilder<Collection<Integer>>) classModel.getProperty("collection"))
                .propertySerialization(new PropertySerialization<Collection<Integer>>() {
                    @Override
                    public boolean shouldSerialize(final Collection<Integer> value) {
                        return true;
                    }
                });
        ((PropertyModelBuilder<Map<String, Double>>) classModel.getProperty("map"))
                .propertySerialization(new PropertySerialization<Map<String, Double>>() {
                    @Override
                    public boolean shouldSerialize(final Map<String, Double> value) {
                        return true;
                    }
                });

        roundTrip(getPojoCodecProviderBuilder(classModel), model,
                "{'collection': null, 'list': [4, 5, 6], 'linked': [7, 8, 9], 'map': null,"
                        + "'concurrent': {'D': 4.4, 'E': 5.5, 'F': 6.6}}");
    }

    @Test
    public void testCanHandleExtraData() {
        decodesTo(getCodec(SimpleModel.class), "{'integerField': 42,  'stringField': 'myString', 'extraFieldA': 1, 'extraFieldB': 2}",
                getSimpleModel());
    }

    @Test
    public void testDataCanHandleMissingData() {
        SimpleModel model = getSimpleModel();
        model.setIntegerField(null);

        decodesTo(getCodec(SimpleModel.class), "{'_t': 'SimpleModel', 'stringField': 'myString'}", model);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testCanHandleTopLevelGenericIfHasCodec() {
        UpperBoundsModel<Long> model = new UpperBoundsModel<Long>(5L);

        ClassModelBuilder<UpperBoundsModel> classModelBuilder = ClassModel.builder(UpperBoundsModel.class);
        ((PropertyModelBuilder<Long>) classModelBuilder.getProperty("myGenericField")).codec(new LongCodec());

        roundTrip(getPojoCodecProviderBuilder(classModelBuilder), model,
                "{'myGenericField': {'$numberLong': '5'}}");
    }

    @Test
    public void testCustomRegisteredPropertyCodecWithValue() {
        CustomPropertyCodecOptionalModel model = new CustomPropertyCodecOptionalModel(Optional.of("foo"));
        roundTrip(getPojoCodecProviderBuilder(CustomPropertyCodecOptionalModel.class).register(new OptionalPropertyCodecProvider()),
                model, "{'optionalField': 'foo'}");
    }

    @Test
    public void testCustomRegisteredPropertyCodecOmittedValue() {
        CustomPropertyCodecOptionalModel model = new CustomPropertyCodecOptionalModel(Optional.<String>empty());
        roundTrip(getPojoCodecProviderBuilder(CustomPropertyCodecOptionalModel.class).register(new OptionalPropertyCodecProvider()),
                model, "{'optionalField': null}");
    }

    @Test
    public void testMapStringObjectModel() {
        MapStringObjectModel model = new MapStringObjectModel(new HashMap<String, Object>(Document.parse("{a : 1, b: 'b', c: [1, 2, 3]}")));
        CodecRegistry registry = fromRegistries(fromCodecs(new MapCodec()),
                fromProviders(getPojoCodecProviderBuilder(MapStringObjectModel.class).build()));
        roundTrip(registry, model, "{ map: {a : 1, b: 'b', c: [1, 2, 3]}}");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMapStringObjectModelWithObjectCodec() {
        MapStringObjectModel model = new MapStringObjectModel(new HashMap<String, Object>(Document.parse("{a : 1, b: 'b', c: [1, 2, 3]}")));
        CodecRegistry registry = fromRegistries(fromCodecs(new MapCodec()), fromCodecs(new ObjectCodec()),
                fromProviders(getPojoCodecProviderBuilder(MapStringObjectModel.class).build()));
        roundTrip(registry, model, "{ map: {a : 1, b: 'b', c: [1, 2, 3]}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testEncodingInvalidMapModel() {
        encodesTo(getPojoCodecProviderBuilder(InvalidMapModel.class), getInvalidMapModel(), "{'invalidMap': {'1': 1, '2': 2}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testDecodingInvalidMapModel() {
        try {
            decodingShouldFail(getCodec(InvalidMapModel.class), "{'invalidMap': {'1': 1, '2': 2}}");
        } catch (CodecConfigurationException e) {
            assertTrue(e.getMessage().startsWith("Could not create a PojoCodec for 'InvalidMapModel'."
                    + " Property 'invalidMap' errored with:"));
            throw e;
        }
    }

    @Test(expected = CodecConfigurationException.class)
    public void testEncodingInvalidCollectionModel() {
        try {
            encodesTo(getPojoCodecProviderBuilder(InvalidCollectionModel.class), new InvalidCollectionModel(asList(1, 2, 3)),
                "{collectionField: [1, 2, 3]}");
        } catch (CodecConfigurationException e) {
            assertTrue(e.getMessage().startsWith("Could not create a PojoCodec for 'InvalidCollectionModel'."
                    + " Property 'collectionField' errored with:"));
            throw e;
        }
    }

    @Test
    public void testInvalidMapModelWithCustomPropertyCodecProvider() {
        encodesTo(getPojoCodecProviderBuilder(InvalidMapModel.class).register(new InvalidMapPropertyCodecProvider()), getInvalidMapModel(),
                "{'invalidMap': {'1': 1, '2': 2}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testConstructorNotPublicModel() {
        decodingShouldFail(getCodec(ConstructorNotPublicModel.class), "{'integerField': 99}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testDataUnknownClass() {
        ClassModel<SimpleModel> classModel = ClassModel.builder(SimpleModel.class).enableDiscriminator(true).build();
        try {
            decodingShouldFail(getCodec(PojoCodecProvider.builder().register(classModel), SimpleModel.class), "{'_t': 'FakeModel'}");
        } catch (CodecConfigurationException e) {
            assertTrue(e.getMessage().startsWith("Failed to decode 'SimpleModel'. Decoding errored with:"));
            throw e;
        }
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForField() {
        decodingShouldFail(getCodec(SimpleModel.class), "{'_t': 'SimpleModel', 'stringField': 123}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForPrimitiveField() {
        decodingShouldFail(getCodec(PrimitivesModel.class), "{ '_t': 'PrimitivesModel', 'myBoolean': null}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForModelField() {
        decodingShouldFail(getCodec(SimpleNestedPojoModel.class), "{ '_t': 'SimpleNestedPojoModel', 'simple': 123}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidDiscriminatorInNestedModel() {
        decodingShouldFail(getCodec(SimpleNestedPojoModel.class), "{ '_t': 'SimpleNestedPojoModel',"
                + "'simple': {'_t': 'FakeModel', 'integerField': 42, 'stringField': 'myString'}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testCannotEncodeUnspecializedClasses() {
        CodecRegistry registry = fromProviders(getPojoCodecProviderBuilder(GenericTreeModel.class).build());
        encode(registry.get(GenericTreeModel.class), getGenericTreeModel());
    }

    @Test(expected = CodecConfigurationException.class)
    public void testCannotDecodeUnspecializedClasses() {
        decodingShouldFail(getCodec(GenericTreeModel.class),
                "{'field1': 'top', 'field2': 1, "
                        + "'left': {'field1': 'left', 'field2': 2, 'left': {'field1': 'left', 'field2': 3}}, "
                        + "'right': {'field1': 'right', 'field2': 4, 'left': {'field1': 'left', 'field2': 5}}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testBsonCreatorPrimitivesAndNullValues() {
        decodingShouldFail(getCodec(CreatorConstructorPrimitivesModel.class), "{intField: 100,  stringField: 'test'}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testCreatorMethodThrowsExceptionModel() {
        decodingShouldFail(getCodec(CreatorMethodThrowsExceptionModel.class),
                "{'integerField': 10, 'stringField': 'eleven', 'longField': {$numberLong: '12'}}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testCreatorConstructorThrowsExceptionModel() {
        decodingShouldFail(getCodec(CreatorConstructorThrowsExceptionModel.class), "{}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidSetterModel() {
        decodingShouldFail(getCodec(InvalidSetterArgsModel.class), "{'integerField': 42, 'stringField': 'myString'}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidGetterAndSetterModelEncoding() {
        InvalidGetterAndSetterModel model = new InvalidGetterAndSetterModel(42, "myString");
        roundTrip(getPojoCodecProviderBuilder(InvalidGetterAndSetterModel.class), model, "{'integerField': 42, 'stringField': 'myString'}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidGetterAndSetterModelDecoding() {
        decodingShouldFail(getCodec(InvalidGetterAndSetterModel.class), "{'integerField': 42, 'stringField': 'myString'}");
    }

    private List<Convention> getDefaultAndUseGettersConvention() {
        List<Convention> conventions = new ArrayList<Convention>(DEFAULT_CONVENTIONS);
        conventions.add(USE_GETTERS_FOR_SETTERS);
        return conventions;
    }

    class ObjectCodec implements Codec<Object> {

        @Override
        public Object decode(final BsonReader reader, final DecoderContext decoderContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void encode(final BsonWriter writer, final Object value, final EncoderContext encoderContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<Object> getEncoderClass() {
            return Object.class;
        }
    }
}
