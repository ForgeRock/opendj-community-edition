#!/bin/sh

# CDDL HEADER START
#
# The contents of this file are subject to the terms of the
# Common Development and Distribution License, Version 1.0 only
# (the "License").  You may not use this file except in compliance
# with the License.
#
# You can obtain a copy of the license at
# trunk/opends/resource/legal-notices/OpenDS.LICENSE
# or https://OpenDS.dev.java.net/OpenDS.LICENSE.
# See the License for the specific language governing permissions
# and limitations under the License.
#
# When distributing Covered Code, include this CDDL HEADER in each
# file and include the License file at
# trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
# add the following below this CDDL HEADER, with the fields enclosed
# information:
#      Portions Copyright [yyyy] [name of copyright owner]
#
# CDDL HEADER END
#
#
#      Portions Copyright 2007-2008 Sun Microsystems, Inc.

echo "Backing configuration up"
me "${tests.config}" "${tests.config.backup}"
echo "Loading configuration as of ${tests.run.time}"
cp "${tests.run.dir}${file.separator}${tests.run.time}${file.separator}config${file.separator}${tests.config.file}" "${tests.config}"
echo "Starting test run"
"${staf.install.dir}${file.separator}bin${file.separator}STAF" local STAX "${tests.request}"
echo "Removing configuration of ${tests.run.time}"
rm -f "${tests.config}"
echo "Restoring original configuration"
mv "${tests.config.backup}" "${tests.config}"
