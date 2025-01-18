/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ygyin.coop.controller;

import com.ygyin.coop.common.BaseResponse;
import com.ygyin.coop.common.ResUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author <a href="mailto:chenxilzx1@gmail.com">theonefx</a>
 */
@Controller
public class BasicController {


    @RequestMapping("/hello/test")
    @ResponseBody
    public BaseResponse<String> helloTest(@RequestParam(name = "name", defaultValue = "unknown user") String name) {
        System.out.println(1 / 0);
        // return new BaseResponse<>(200, "Hello " + name);
        return ResUtils.success("Hello " + name);
    }

}
