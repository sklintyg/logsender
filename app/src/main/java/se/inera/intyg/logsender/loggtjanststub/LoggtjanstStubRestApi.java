/*
 * Copyright (C) 2025 Inera AB (http://www.inera.se)
 *
 * This file is part of sklintyg (https://github.com/sklintyg).
 *
 * sklintyg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * sklintyg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.inera.intyg.logsender.loggtjanststub;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import se.riv.informationsecurity.auditing.log.v2.LogType;

@Path("/")
public class LoggtjanstStubRestApi {

  @Autowired
  private LogStore logStore;

  @Autowired
  private StubState stubState;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<LogType> getAllLogEntries() {
    return logStore.getAll();
  }

  @DELETE
  @Path("/logs")
  public Response deleteLogStore() {
    logStore.clear();
    stubState.resetBatchCount();
    return Response.ok().build();
  }

  @GET
  @Path("/batch-count")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getBatchCount() {
    return Response.ok().entity("{\"batchCount\":" + stubState.getBatchCount() + "}").build();
  }

  @GET
  @Path("/online")
  @Produces(MediaType.APPLICATION_JSON)
  public Response activateStub() {
    stubState.setActive(true);
    return Response.ok().entity("{\"status\":\"OK\",\"active\":true}").build();
  }

  @GET
  @Path("/offline")
  @Produces(MediaType.APPLICATION_JSON)
  public Response deactivateStub() {
    stubState.setActive(false);
    return Response.ok().entity("{\"status\":\"OK\",\"active\":false}").build();
  }

  @GET
  @Path("/logs")
  public Response getAllLogs() {
    return Response.ok().entity(logStore.getAll()).build();
  }

  @GET
  @Path("/error/{errorType}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response activateErrorState(@PathParam("errorType") String errorType) {
    try {
      ErrorState errorState = ErrorState.valueOf(errorType);
      stubState.setErrorState(errorState);
      return Response.ok().entity("{\"status\":\"OK\",\"errorState\":\"" + errorType + "\"}")
          .build();
    } catch (IllegalArgumentException e) {
      return Response.serverError().entity(
              "{\"status\":\"ERROR\",\"message\":\"Unknown ErrorState: " + errorType
                  + ". Allowed values are NONE, ERROR, VALIDATION\"}")
          .build();
    }
  }


  @GET
  @Path("/latency/{latencyMillis}")
  public Response setLatency(@PathParam("latencyMillis") Long latencyMillis) {
    stubState.setArtificialLatency(latencyMillis);
    return Response.ok().entity("OK").build();
  }
}
