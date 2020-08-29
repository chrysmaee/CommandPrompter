/*
 * MIT License
 *
 * Copyright (c) 2020 Ethan Bacurio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.cyr1en.cp.api.prompt;

import com.cyr1en.cp.CommandPrompter;
import com.cyr1en.cp.prompt.PromptContext;
import com.cyr1en.cp.prompt.PromptQueue;

public interface Prompt {

  /**
   * Method that sends the prompt.
   */
  void sendPrompt();

  /**
   * Accessor for the {@link PromptContext}.
   *
   * <p>{@link PromptContext} contains all the information that you need for
   * the {@link Prompt}.</p>
   *
   * @see PromptContext
   * @return context for the prompt.
   */
  PromptContext getContext();

  /**
   * Get the instance of the queue where this prompt exists.
   *
   * @return Get the instance of the queue where this prompt exists.
   */
  PromptQueue getPromptQueue();

  /**
   * Get instance of the plugin.
   *
   * <p>The instance is a sub-class {@link org.bukkit.plugin.java.JavaPlugin}.</p>
   *
   * @return Instance of a plugin.
   */
  CommandPrompter getPlugin();

  /**
   * Get the identifier for the prompt.
   *
   * An identifier is a {@link String} that signifies the type of a prompt.
   *
   * @return Returns the identifier for the prompt.
   */
  String getTrigger();


}