package com.proto.lang;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnippetLoader {

  private final Random random = new Random();

  public String loadRandom(String fileName) {

    List<String> items = new ArrayList<>();

    try {

      String path = "/snippets/" + fileName;

      InputStream inputStream = getClass().getResourceAsStream(path);

      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

      String line;

      while ((line = reader.readLine()) != null) {

        if (!line.trim().isEmpty()) {
          items.add(line);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    if (items.isEmpty()) {
      return "No content found";
    }

    return items.get(random.nextInt(items.size()));
  }
}
