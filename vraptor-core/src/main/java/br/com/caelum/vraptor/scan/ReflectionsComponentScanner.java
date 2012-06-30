package br.com.caelum.vraptor.scan;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.filter;
import static com.google.common.collect.Sets.union;
import static org.reflections.util.ClasspathHelper.forWebInfClasses;
import static org.reflections.util.ClasspathHelper.forWebInfLib;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.caelum.vraptor.core.BaseComponents;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

/**
 * A component scanner using Reflections API.
 * 
 * @author Otávio Scherer Garcia
 * @author Fabio Kung
 * @since 3.5
 */
public class ReflectionsComponentScanner implements ComponentScanner {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionsComponentScanner.class);
    private final ServletContext context;

    public ReflectionsComponentScanner(ServletContext context) {
        this.context = context;
    }

    public Collection<String> scan(ClasspathResolver resolver) {
        Set<Class<?>> webinfClassesComponents = findComponentsFromWebinfClasses();
        Set<Class<?>> webinfLibComponents = findComponentsFromWebinfLib(resolver.findBasePackages());
        Set<Class<?>> allComponents = union(webinfClassesComponents, webinfLibComponents);

        logger.debug("Found {} components inside classes, {} components inside libs");

        // TODO return class instead string
        return transform(allComponents, new Function<Class<?>, String>() {
            public String apply(Class<?> clazz) {
                return clazz.getName();
            }
        });
    }

    /**
     * Find all components inside WEB-INF/classes.
     */
    private Set<Class<?>> findComponentsFromWebinfClasses() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(forWebInfClasses(context))
            .setScanners(new TypeAnnotationsScanner()));

        Set<Class<?>> components = new HashSet<Class<?>>();
        for (Class<? extends Annotation> stereotype : BaseComponents.getStereotypes()) {
            components.addAll(reflections.getTypesAnnotatedWith(stereotype));
        }

        return components;
    }

    /**
     * Find components inside WEB-INF/lib under the packages defined into packages parameter.
     */
    private Set<Class<?>> findComponentsFromWebinfLib(final List<String> basePackages) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(forWebInfLib(context))
            .setScanners(new TypeAnnotationsScanner()));

        Set<Class<?>> components = new HashSet<Class<?>>();
        for (Class<? extends Annotation> stereotype : BaseComponents.getStereotypes()) {
            components.addAll(reflections.getTypesAnnotatedWith(stereotype));
        }

        return filter(components, new Predicate<Class<?>>() {
            public boolean apply(Class<?> clazz) {
                for (String current : basePackages) {
                    logger.debug("found component {}, check if is inside {} package", clazz.getName(), current);
                    if (clazz.getName().startsWith(current + ".")) {
                        return true;
                    }
                }

                return false;
            }
        });
    }
}
