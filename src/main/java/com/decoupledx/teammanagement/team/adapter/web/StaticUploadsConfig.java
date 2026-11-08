package com.decoupledx.teammanagement.team.adapter.web;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serves uploaded images (player photos, team logos) from the configured
 * upload directory as /uploads/** (ADR-0002). The directory is created on
 * first upload by the media endpoints; the security chain permitAll's the
 * read-only /uploads paths — file names are generated UUIDs, so unguessable.
 */
@Configuration
class StaticUploadsConfig implements WebMvcConfigurer {

    StaticUploadsConfig(@Value("${app.uploads.dir:uploads}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    private final String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Path.of(uploadDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
