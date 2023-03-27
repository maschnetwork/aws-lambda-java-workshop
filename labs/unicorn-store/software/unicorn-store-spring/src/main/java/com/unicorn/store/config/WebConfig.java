package com.unicorn.store.config;

import com.unicorn.store.otel.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.contrib.awsxray.AwsXrayIdGenerator;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.awssdk.v2_2.AwsSdkTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${aws.local.endpoint:#{null}}")
    private String endpoint = "";

    @Value("${cloud.aws.region.static:#{null}}")
    private String region = "";

    @Bean
    public RandomNumberGenerator randomNumberGenerator() {
        return new PseudoRandomNumberGenerator();
    }

    @Bean
    public OpenTelemetry getOpenTelemetry() {

        // Extract OpenTelemetry variables
        AttributesBuilder builder = Attributes.builder();
        // https://opentelemetry.io/docs/reference/specification/resource/sdk/#specifying-resource-information-via-an-environment-variable
        Arrays.stream(System.getenv("OTEL_RESOURCE_ATTRIBUTES")
                        .split(","))
                .map( pair -> pair.split("=") )
                .map( keyValue -> Attributes.of(AttributeKey.stringKey(keyValue[0]), keyValue[1]))
                .forEach(builder::putAll);
        // https://opentelemetry.io/docs/concepts/sdk-configuration/otlp-exporter-configuration/#otel_exporter_otlp_endpoint
        String exporterEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");

        Resource resource = Resource.getDefault().merge(
                Resource.create(builder.build()));
        return OpenTelemetrySdk.builder()
                // This will enable your downstream requests to include the X-Ray trace header
                .setPropagators(
                        ContextPropagators.create(
                                TextMapPropagator.composite(
                                        W3CTraceContextPropagator.getInstance(), AwsXrayPropagator.getInstance())))

                // This provides basic configuration of a TracerProvider which generates X-Ray compliant IDs
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .setResource(resource)
                                .addSpanProcessor(
                                        BatchSpanProcessor.builder(
                                                OtlpGrpcSpanExporter.builder()
                                                        .setEndpoint(exporterEndpoint)
                                                        .build()
                                        ).build())
                                .setIdGenerator(AwsXrayIdGenerator.getInstance())
                                .build())
                .buildAndRegisterGlobal();
    }

    @Bean
    public Tracer getTracer(OpenTelemetry otel) {
        return otel.getTracer("unicornstore");
    }


    private Tracer tracer;
    @Autowired
    public void setTracer(@Lazy Tracer tracer) {
        this.tracer = tracer;
    }

    private OpenTelemetry openTelemetry;

    @Autowired
    public void setOpenTelemetry(@Lazy OpenTelemetry otel){
        this.openTelemetry = otel;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TracingRequestInterceptor(openTelemetry, tracer));
        WebMvcConfigurer.super.addInterceptors(registry);
    }

    @Bean
    public EventBridgeClient eventBridge(OpenTelemetry otel) {
        return EventBridgeClient.builder()
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(AwsSdkTelemetry.create(otel).newExecutionInterceptor())
                                .build())
                .build();
    }

    @Bean
    public S3Client amazonS3(OpenTelemetry otel) {
        return S3Client.builder()
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(AwsSdkTelemetry.create(otel).newExecutionInterceptor())
                                .build())
                .build();
    }

    @Bean
    public DynamoDbClient amazonDynamoDB(OpenTelemetry otel) {

        return DynamoDbClient.builder()
                .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                .addExecutionInterceptor(AwsSdkTelemetry.create(otel).newExecutionInterceptor())
                                .build())
                .build();
    }

    @Bean
    public MetricEmitter metricEmitter(OpenTelemetry otel) {
        return new MetricEmitter(otel);
    }

    @Bean
    public FilterRegistrationBean<ApplicationFilter> filterRegistrationBean(MetricEmitter metricEmitter) {
        FilterRegistrationBean<ApplicationFilter> filterBean = new FilterRegistrationBean<>();
        filterBean.setFilter(new ApplicationFilter(metricEmitter));
        filterBean.setUrlPatterns(Arrays.asList("/api"));
        return filterBean;
    }

}

