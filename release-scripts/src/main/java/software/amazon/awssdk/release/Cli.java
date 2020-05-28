/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.release;

import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import software.amazon.awssdk.utils.Logger;

public abstract class Cli {
    private final Logger log = Logger.loggerFor(Cli.class);
    private final Option[] optionsToAdd;

    public Cli(Option... optionsToAdd) {
        this.optionsToAdd = optionsToAdd;
    }

    public final void run(String[] args) {
        Options options = new Options();
        Stream.of(optionsToAdd).forEach(options::addOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter help = new HelpFormatter();

        try {
            CommandLine commandLine = parser.parse(options, args);
            run(commandLine);
        } catch (ParseException e) {
            log.error(() -> "Invalid input: " + e.getMessage());
            help.printHelp(getClass().getSimpleName(), options);
            throw new Error();
        } catch (Exception e) {
            log.error(() -> "Script execution failed.", e);
            throw new Error();
        }
    }

    protected static Option requiredOption(String longCommand, String description) {
        Option option = optionalOption(longCommand, description);
        option.setRequired(true);
        return option;
    }

    protected static Option optionalOption(String longCommand, String description) {
        return new Option(null, longCommand, true, description);
    }

    protected abstract void run(CommandLine commandLine) throws Exception;
}
