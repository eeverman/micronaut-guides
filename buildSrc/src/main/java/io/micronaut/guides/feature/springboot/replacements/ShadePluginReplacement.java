package io.micronaut.guides.feature.springboot.replacements;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.starter.application.ApplicationType;
import io.micronaut.starter.feature.Feature;
import io.micronaut.starter.feature.other.ShadePlugin;
import io.micronaut.starter.options.Options;
import jakarta.inject.Singleton;

import java.util.Set;

import static io.micronaut.guides.feature.springboot.SpringBootApplicationFeature.isSpringBootApplication;

@Singleton
@Replaces(ShadePlugin.class)
public class ShadePluginReplacement extends ShadePlugin  {
    @Override
    public boolean shouldApply(
            ApplicationType applicationType,
            Options options,
            Set<Feature> selectedFeatures) {
        return super.shouldApply(applicationType, options, selectedFeatures) &&
                !isSpringBootApplication(selectedFeatures);
    }

}
