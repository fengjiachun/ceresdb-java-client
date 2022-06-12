/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* Generated by: ParserGeneratorCC: Do not edit this line. CharStream.java Version 1.1 */
/* ParserGeneratorCCOptions:SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.ceresdb.sql;

/**
 * This interface describes a character stream that maintains line and
 * column number positions of the characters.  It also has the capability
 * to backup the stream to some extent.  An implementation of this
 * interface is used in the TokenManager implementation generated by
 * JavaCCParser.
 *
 * All the methods except backup can be implemented in any fashion. backup
 * needs to be implemented correctly for the correct operation of the lexer.
 * Rest of the methods are all used to get information like line number,
 * column number and the String that constitutes a token and are not used
 * by the lexer. Hence their implementation won't affect the generated lexer's
 * operation.
 */

public interface CharStream {
    /**
     * Get the next character from the selected input. The method
     * of selecting the input is the responsibility of the class
     * implementing this interface.
     * @return the next character from the selected input
     * @throws java.io.IOException on IO error
     */
    char readChar() throws java.io.IOException;

    /**
     * @return the column number of the first character for current token (being
     * matched after the last call to beginToken).
     */
    int getBeginColumn();

    /**
     * @return the line number of the first character for current token (being
     * matched after the last call to BeginToken).
     */
    int getBeginLine();

    /**
     * @return the column number of the last character for current token (being
     * matched after the last call to BeginToken).
     */
    int getEndColumn();

    /**
     * @return the line number of the last character for current token (being
     * matched after the last call to BeginToken).
     */
    int getEndLine();

    /**
     * Backs up the input stream by amount steps. Lexer calls this method if it
     * had already read some characters, but could not use them to match a
     * (longer) token. So, they will be used again as the prefix of the next
     * token and it is the implemetation's responsibility to do this right.
     * @param amount Number of chars to back up.
     */
    void backup(int amount);

    /**
     * @return the next character that marks the beginning of the next token.
     * All characters must remain in the buffer between two successive calls
     * to this method to implement backup correctly.
     */
    char beginToken() throws java.io.IOException;

    /**
     * @return a string made up of characters from the marked token beginning
     * to the current buffer position. Implementations have the choice of returning
     * anything that they want to. For example, for efficiency, one might decide
     * to just return null, which is a valid implementation.
     */
    String getImage();

    /**
     * @return an array of characters that make up the suffix of length 'len' for
     * the currently matched token. This is used to build up the matched string
     * for use in actions in the case of MORE. A simple and inefficient
     * implementation of this is as follows:
     * <pre>
     *   {
     *      String t = getImage();
     *      return t.substring(t.length() - len, t.length()).toCharArray();
     *   }
     * </pre>
     */
    char[] getSuffix(int len);

    /**
     * The lexer calls this function to indicate that it is done with the stream
     * and hence implementations can free any resources held by this class.
     * Again, the body of this function can be just empty and it will not
     * affect the lexer's operation.
     */
    void done();

    // Getters and setters

    /**
     * @return Current tab size.
     */
    int getTabSize();

    /**
     * Set the tab size to use.
     * @param i spaces per tab
     */
    void setTabSize(int i);

    /**
     * @return <code>true</code> if line number and column numbers should be tracked.
     */
    boolean isTrackLineColumn();

    /**
     * Enable or disable line number and column number tracking.
     * @param trackLineColumn <code>true</code> to track it, <code>false</code> to not do it.
     */
    void setTrackLineColumn(boolean trackLineColumn);
}
/* ParserGeneratorCC - OriginalChecksum=a0f02fb813390766c4c59bdd9cd91295 (do not edit this line) */
