package com.demo.uctp.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {
  @GetMapping("/")
  public String home() { return "redirect:/uctp-buttonless"; }

  @GetMapping("/uctp-buttonless")
  public String buttonless() { return "uctp-buttonless"; }
}
