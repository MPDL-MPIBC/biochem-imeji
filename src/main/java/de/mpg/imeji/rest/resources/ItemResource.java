package de.mpg.imeji.rest.resources;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import de.mpg.imeji.rest.process.ItemProcess;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.resources.ImejiResource;
import de.mpg.imeji.rest.to.JSONResponse;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.InputStream;

@Path("/items")
@Api(value = "rest/items", description = "Operations on items")
public class ItemResource implements ImejiResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response readAll(@Context HttpServletRequest req) {
		return null;
	}

	@GET
	@Path("/{id}")
	@ApiOperation(value = "Get item by id")
	@Produces(MediaType.APPLICATION_JSON)
	public Response readFromID(@Context HttpServletRequest req,
			@PathParam("id") String id) {
		JSONResponse resp = ItemProcess.readItem(req, id);
		return RestProcessUtils.buildJSONResponse(resp);
	}

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@ApiOperation(value = "Create new item with a File", notes = "Create an item with a file. File can be defined either as (by order of priority):"
			+ "<br/> 1) form parameter <br/> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) "
			+ "<br/> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)"
			+ "<br/><br/>"
			+ "Json example:"
			+ "<br/>{"
			+ "<br/>\"collectionId\" : \"abc123\","
			+ "<br/>\"fetchUrl\" : \"http://example.org/myFile.png\","
			+ "<br/>\"referenceUrl\" : \"http://example.org/myFile.png\","
			+ "<br/>\"metadata\" : []"
			+ "<br/>}"
			+ "<br/><br/>"
			+ "The metadata parameter allows to define the metadata of item during the creation of the item. To get an example of how to do it, please try the get item method")
	@Produces(MediaType.APPLICATION_JSON)
	public Response create(@Context HttpServletRequest req,
			@FormDataParam("file") InputStream file,
			@ApiParam(required = true) @FormDataParam("json") String json,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		String filename = fileDetail != null ? fileDetail.getFileName() : null;
		return RestProcessUtils.buildJSONResponse(ItemProcess.createItem(req,
				file, json, filename));
	}

	public Response create(HttpServletRequest req) {
		return null;
	}

	@DELETE
	@Path("/{id}")
	@ApiOperation(value = "Delete item by id")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
		JSONResponse resp = ItemProcess.deleteItem(req, id);
		return RestProcessUtils.buildJSONResponse(resp);
	}

}