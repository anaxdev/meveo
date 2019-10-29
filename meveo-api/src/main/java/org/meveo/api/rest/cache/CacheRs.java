/*
 * (C) Copyright 2018-2020 Webdrone SAS (https://www.webdrone.fr/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. This program is
 * not suitable for any direct or indirect application in MILITARY industry See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.meveo.api.rest.cache;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.meveo.cache.CustomFieldsCacheContainerProvider;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Path("/caches")
@Api("Cache")
@Produces(MediaType.APPLICATION_JSON)
public class CacheRs {

    @Inject
    private CustomFieldsCacheContainerProvider cache;

    @POST
    @Path("/refresh")
    @ApiOperation("Clean and populate all caches")
    public void refreshAll() {
        cache.refreshCache(null);
    }

    @POST
    @Path("/populate")
    @ApiOperation("Populate all caches")
    public void populateAll() {
        cache.populateCache(null, false);
    }

    @POST
    @Path("/{name}/refresh")
    @ApiOperation("Clean and popuplate a cache")
    public void refresh(@PathParam("name") @ApiParam(value = "Name of the cache to refresh", example = "meveo-cft-cache") String cacheName) {
        cache.refreshCache(cacheName);
    }

    @POST
    @Path("/{name}/populate")
    @ApiOperation("Populate a cache")
    public void populate(@PathParam("name") @ApiParam(value = "Name of the cache to populate", example = "meveo-cft-cache") String cacheName) {
        cache.populateCache(cacheName, false);
    }

    @GET
    @Path("/{name}/status")
    @ApiOperation("Count elements inside a cache")
    public int getNbElements(@PathParam("name") @ApiParam(value = "Name of the cache to count elements", example = "meveo-cft-cache") String cacheName) {
        return this.cache.getCaches().get(cacheName).size();
    }

    @GET
    @Path("/status")
    @ApiOperation("Count elements by cache")
    public Map<String, Integer> getNbElementByCacheName() {
        return this.cache.getCaches().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, c -> c.getValue().size()));
    }

}