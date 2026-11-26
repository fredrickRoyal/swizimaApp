"use strict";
// Copyright 2024 Google LLC. Use of this source code is governed by an
// MIT-style license that can be found in the LICENSE file or at
// https://opensource.org/licenses/MIT.
Object.defineProperty(exports, "__esModule", { value: true });
exports.Version = void 0;
exports.compareVersions = compareVersions;
class Version {
    major;
    minor;
    patch;
    constructor(major, minor, patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }
    static parse(version) {
        const match = version.match(/^(\d+)\.(\d+)\.(\d+)$/);
        if (match === null) {
            throw new Error(`Invalid version ${version}`);
        }
        return new Version(parseInt(match[1]), parseInt(match[2]), parseInt(match[3]));
    }
    toString() {
        return `${this.major}.${this.minor}.${this.patch}`;
    }
}
exports.Version = Version;
/**
 * Returns a negative number if `version1` comes before `version2` in semantic
 * order, a positive number if `version2` comes before `version1`, and 0 if
 * they're equal.
 */
function compareVersions(version1, version2) {
    const major = version1.major - version2.major;
    if (major !== 0)
        return major;
    const minor = version1.minor - version2.minor;
    if (minor !== 0)
        return minor;
    return version1.patch - version2.patch;
}
//# sourceMappingURL=version.js.map