package org.zalando.riptide;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nullable;
import java.net.URI;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.springframework.web.util.UriComponentsBuilder.fromUri;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

public interface RequestArguments {

    URI getBaseUrl();

    HttpMethod getMethod();

    String getUriTemplate();

    ImmutableList<Object> getUriVariables();

    URI getUri();

    ImmutableMultimap<String, String> getQueryParams();

    URI getRequestUri();

    ImmutableMultimap<String, String> getHeaders();

    Object getBody();

    RequestArguments withBaseUrl(@Nullable URI baseUrl);

    RequestArguments withMethod(@Nullable HttpMethod method);

    RequestArguments withUriTemplate(@Nullable String uriTemplate);

    RequestArguments withUriVariables(@Nullable ImmutableList<Object> uriVariables);

    RequestArguments withUri(@Nullable URI uri);

    RequestArguments withQueryParams(@Nullable ImmutableMultimap<String, String> queryParams);

    RequestArguments withRequestUri(@Nullable URI requestUri);

    RequestArguments withHeaders(@Nullable ImmutableMultimap<String, String> headers);

    RequestArguments withBody(@Nullable Object body);

    default RequestArguments withRequestUri() {
        final URI uri = getUri();

        final URI toResolve;
        if (uri != null) {
            toResolve = uri;
        } else {
            final String uriTemplate = getUriTemplate();
            if (uriTemplate != null) {
                // expand uri template
                toResolve = fromUriString(uriTemplate)
                        .buildAndExpand(getUriVariables().toArray())
                        .encode()
                        .toUri();
            } else {
                toResolve = null;
            }
        }

        final URI baseUrl = getBaseUrl();

        final URI resolved;
        if (toResolve == null) {
            resolved = checkNotNull(baseUrl, "base url required");
        } else if (baseUrl == null) {
            resolved = toResolve;
        } else {
            resolved = baseUrl.resolve(toResolve);
        }

        // encode query params
        final MultiValueMap<String, String> queryParams;
        {
            final UriComponentsBuilder components = UriComponentsBuilder.newInstance();
            getQueryParams().entries().forEach(entry ->
                    components.queryParam(entry.getKey(), entry.getValue()));
            queryParams = components.build().encode().getQueryParams();
        }

        // build request uri
        final URI requestUri = fromUri(resolved)
                .queryParams(queryParams)
                .build(true).normalize().toUri();

        return withRequestUri(requestUri);
    }

    static RequestArguments create() {
        return new DefaultRequestArguments(null, null, null, ImmutableList.of(), null, ImmutableMultimap.of(), null,
                ImmutableMultimap.of(), null);
    }

}
