package com.orange.oswe.demo.trio.mvc.error;

import lombok.Value;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = {AbstractGlobalErrorHandler.class, ErrorHandlingTest.TestController.class})
public class ErrorHandlingTest {

    @Controller
    public static class TestController {
        @Value
        public static class Dummy {
            public final String content;
        }
        @RequestMapping(value = "/dummies", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
        @ResponseBody
        public List<Dummy> getAllJson() {
            return Arrays.asList(new Dummy("1"), new Dummy("2"), new Dummy("3"));
        }
        @RequestMapping(value = "/dummies", method = RequestMethod.GET, produces = {MediaType.TEXT_HTML_VALUE, MediaType.APPLICATION_XHTML_XML_VALUE})
        @ResponseBody
        public ModelAndView getAllHtml() {
            return new ModelAndView("dummies");
        }
        @RequestMapping(value = "/dummies/filtered", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
        @ResponseBody
        public List<Dummy> getFiltered(@RequestParam("param") Integer param) {
            return Arrays.asList(new Dummy("1"), new Dummy("2"));
        }
    }
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void get_dummies_json_should_work() throws Exception {
        mockMvc.perform(get("/dummies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
    @Test
    public void get_filtered_without_param_should_return_400() throws Exception {
        mockMvc.perform(get("/dummies/filtered").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.MissingParameter.name()))
                .andExpect(jsonPath("$.code").value(ErrorCode.MissingParameter.getCode()))
                .andExpect(jsonPath("$.timestamp").doesNotExist());// not the base Spring exception
    }
    @Test
    public void get_filtered_with_bad_param_type_should_return_400() throws Exception {
        mockMvc.perform(get("/dummies/filtered").param("param", "not_an_int").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.InvalidParameter.name()))
                .andExpect(jsonPath("$.code").value(ErrorCode.InvalidParameter.getCode()))
                .andExpect(jsonPath("$.timestamp").doesNotExist());// not the base Spring exception
    }
    @Test
    public void post_dummies_should_return_405_json() throws Exception {
        mockMvc.perform(post("/dummies").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.METHOD_NOT_ALLOWED.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.MethodNotSupported.name()))
                .andExpect(jsonPath("$.code").value(ErrorCode.MethodNotSupported.getCode()))
                .andExpect(jsonPath("$.timestamp").doesNotExist());// not the base Spring exception
    }
    @Test
    public void get_dummies_html_should_work() throws Exception {
        mockMvc.perform(get("/dummies").accept(MediaType.TEXT_HTML))
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(MockMvcResultMatchers.view().name("dummies"));
    }
    @Test
    public void unmapped_path_should_return_404_json() throws Exception {
        mockMvc.perform(get("/no_such_path").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(ErrorCode.ServiceNotFound.name()))
                .andExpect(jsonPath("$.code").value(ErrorCode.ServiceNotFound.getCode()))
                .andExpect(jsonPath("$.timestamp").doesNotExist());// not the base Spring exception
    }

    @Test
    public void unmapped_path_should_return_404_page() throws Exception {
        mockMvc.perform(get("/no_such_path").accept(MediaType.TEXT_HTML))
                .andExpect(status().is(HttpStatus.NOT_FOUND.value()))
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

}