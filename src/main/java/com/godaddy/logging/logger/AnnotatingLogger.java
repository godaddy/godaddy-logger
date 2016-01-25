/**
 * Copyright (c) 2015 GoDaddy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.godaddy.logging.logger;

import com.godaddy.logging.LogContext;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggingConfigs;
import com.godaddy.logging.MessageBuilder;

public class AnnotatingLogger extends LoggerImpl {

    private final Logger root;
    private final LoggerImpl parent;
    private Object obj;
    private final LoggingConfigs configs;


    public AnnotatingLogger(Logger root, LoggerImpl parent, Object obj, final LoggingConfigs configs) {
        super(root, configs);
        this.root = root;
        this.parent = parent;

        this.obj = obj;
        this.configs = configs;
    }

    @Override
    public LogContext<?> getMessage(LogContext<?> previous) {
        MessageBuilder messageBuilder = configs.getMessageBuilderFunction().getBuilder(configs);

        return parent.getMessage(messageBuilder.buildMessage(previous, obj));
    }
}
