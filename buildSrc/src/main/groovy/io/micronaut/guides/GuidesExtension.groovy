package io.micronaut.guides

import groovy.transform.CompileStatic
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
class GuidesExtension {

    final Property<File> output

    @Inject
    GuidesExtension(ObjectFactory objects) {
        output = objects.property(File)
    }
}
