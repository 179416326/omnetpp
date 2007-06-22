//==========================================================================
//  SECTIONBASEDCONFIG.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2005 Andras Varga

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <assert.h>
#include <algorithm>
#include <sstream>
#include "sectionbasedconfig.h"
#include "cconfigreader.h"
#include "patternmatcher.h"
#include "valueiterator.h"
#include "cexception.h"
#include "scenario.h"
#include "globals.h"
#include "cconfigentry.h"


//XXX optimize storage (now keys with wildcard groupName are stored multiple times, in several groups)
//XXX repetitions=10
//XXX make samples/database work again!
//XXX check behavior of keys which don't contain a dot! or: forbid them?
//XXX the likes of: **.apply-default, **whatever.apply=default, whatever**.apply-default!!! make them illegal?
//XXX if Cmdenv is not linked in, all "cmdenv" config keys throw an error!!! ignore "cmdenv-*" when no such key is registered?

Register_PerRunConfigEntry(CFGID_EXTENDS, "extends", CFG_STRING, NULL, "XXX todo");


std::string SectionBasedConfiguration::KeyValue1::nullbasedir;


SectionBasedConfiguration::SectionBasedConfiguration()
{
    ini = NULL;
    activeRunNumber = 0;
}

SectionBasedConfiguration::~SectionBasedConfiguration()
{
    clear();
    delete ini;
}

void SectionBasedConfiguration::setConfigurationReader(cConfigurationReader *ini)
{
    clear();
    this->ini = ini;
    nullEntry.setBaseDirectory(ini->getDefaultBaseDirectory());
    validateConfig();
}

void SectionBasedConfiguration::clear()
{
    // note: this gets called between activateConfig() calls, so "ini" must NOT be NULL'ed out here
    activeConfig = "";
    activeRunNumber = 0;
    config.clear();
    groups.clear();
    wildcardGroup.entries.clear();
}

void SectionBasedConfiguration::initializeFrom(cConfiguration *conf)
{
    //XXX
}

const char *SectionBasedConfiguration::getFileName() const
{
    return ini==NULL ? NULL : ini->getFileName();
}

const char *SectionBasedConfiguration::getActiveConfigName() const
{
    return activeConfig.c_str();
}

int SectionBasedConfiguration::getActiveRunNumber() const
{
    return activeRunNumber;
}

std::vector<std::string> SectionBasedConfiguration::getConfigNames()
{
    // use a set so that [Config X] and [Scenario X] only produce one X in the result
    std::set<std::string> uniqueNames;
    for (int i=0; i<ini->getNumSections(); i++)
    {
        const char *section = ini->getSectionName(i);
        if (strcmp(section, "General")==0)
            uniqueNames.insert(section);
        else if (strncmp(section, "Config ", 7)==0)
            uniqueNames.insert(section+7);
        else if (strncmp(section, "Scenario ", 9)==0)
            uniqueNames.insert(section+9);
    }
    std::vector<std::string> result;
    for (std::set<std::string>::iterator it=uniqueNames.begin(); it!=uniqueNames.end(); it++)
        result.push_back(*it);
    return result;

}

std::string SectionBasedConfiguration::getConfigDescription(const char *scenarioOrConfigName) const
{
    int sectionId = resolveConfigName(scenarioOrConfigName);
    if (sectionId == -1)
        throw cRuntimeError("No such config or scenario: %s", scenarioOrConfigName);

    // determine the list of sections, from this one up to [General]
    const char *section = ini->getSectionName(sectionId);
    std::vector<int> sectionChain = resolveSectionChain(section);

    // walk the list of fallback sections, and return the first "description" entry we meet
    for (int i=0; i<sectionChain.size(); i++)
    {
        int entryId = internalFindEntry(sectionChain[i], "description");
        const char *desc = entryId == -1 ? NULL : ini->getEntry(sectionChain[i], entryId).getValue();
        if (desc != NULL)
            return desc;
    }
    return "";
}

std::string SectionBasedConfiguration::getBaseConfig(const char *scenarioOrConfigName) const
{
    int sectionId = resolveConfigName(scenarioOrConfigName);
    if (sectionId == -1)
        throw cRuntimeError("No such config or scenario: %s", scenarioOrConfigName);
    int entryId = internalFindEntry(sectionId, "extends");
    std::string extends = entryId==-1 ? "" : ini->getEntry(sectionId, entryId).getValue();
    if (extends.empty())
        extends = "General";
    int baseSectionId = resolveConfigName(extends.c_str());
    return baseSectionId==-1 ? "" : extends;
}

int SectionBasedConfiguration::resolveConfigName(const char *scenarioOrConfigName) const
{
    if (!scenarioOrConfigName || !scenarioOrConfigName[0])
        throw cRuntimeError("Empty config name specified");
    int id = -1;
    if (!strcmp(scenarioOrConfigName, "General"))
        id = internalFindSection("General");
    if (id == -1)
        id = internalFindSection((std::string("Scenario ")+scenarioOrConfigName).c_str());
    if (id == -1)
        id = internalFindSection((std::string("Config ")+scenarioOrConfigName).c_str());
    return id;
}

void SectionBasedConfiguration::activateConfig(const char *scenarioOrConfigName, int runNumber)
{
    clear();

    activeConfig = scenarioOrConfigName==NULL ? "" : scenarioOrConfigName;
    activeRunNumber = 0;

    int sectionId = resolveConfigName(scenarioOrConfigName);
    if (sectionId == -1 && !strcmp(scenarioOrConfigName, "General"))
        return;  // allow activating "General" even if it's empty
    if (sectionId == -1)
        throw cRuntimeError("No such config or scenario: %s", scenarioOrConfigName);
    if (ini->getSectionName(sectionId)[0] != 'S')  // "Scenario...", not "Config..." or "General"
        doActivateConfig(sectionId);
    else
        doActivateScenario(sectionId, runNumber);
}

void SectionBasedConfiguration::doActivateConfig(int sectionId)
{
    std::string section = ini->getSectionName(sectionId);

    // determine the list of sections, from this one up to [General]
    std::vector<int> sectionChain = resolveSectionChain(section.c_str());

    // walk the list of fallback sections, and add entries to our tables (config[], groups[], etc).
    // Entries added first will have precedence over those added later.
    for (int i=0; i<sectionChain.size(); i++)
        for (int j=0; j<ini->getNumEntries(sectionChain[i]); j++)
            addEntry(convert(ini->getEntry(sectionChain[i], j)));
}

void SectionBasedConfiguration::doActivateScenario(int sectionId, int runNumber)
{
    std::string section = ini->getSectionName(sectionId);

    // extract all iteration specs from values within this section
    std::vector<IterationSpec> iterspecs = collectIterationSpecs(sectionId);
    validateIterations(iterspecs);

    // see if there's a condition given
    int conditionEntryId = internalFindEntry(sectionId, "condition");
    const char *condition = conditionEntryId!=-1 ? ini->getEntry(sectionId, conditionEntryId).getValue() : NULL;

    // determine the values to substitute into the iteration specs (${...})
    std::vector<std::string> values;
    try {
        values = Scenario(iterspecs, condition).generate(runNumber);
    } catch (std::exception& e) {
        throw cRuntimeError("Scenario generator: %s", e.what());
    }

    // add the resulting entries into the tables (config[], params[]),
    // substituting the iteration values
    for (int entryId=0; entryId<ini->getNumEntries(sectionId); entryId++)
    {
        const cConfigurationReader::KeyValue& entry = ini->getEntry(sectionId, entryId);
        std::string value = entry.getValue();
        std::string actualValue = substitute(value, entryId, iterspecs, values);

        // add entry to our tables
        KeyValue1 actualEntry = convert(entry);
        actualEntry.value = actualValue;
        addEntry(actualEntry);
    }

    // determine the list of sections, from this one up to [General]
    std::vector<int> sectionChain = resolveSectionChain(section.c_str());

    // walk the list of fallback sections, and add entries to out tables (config[] and params[]).
    // Entries added first will have precedence over those added later.
    // NOTE: Loop goes from 1, so we skip the Scenario section we already processed above.
    for (int i=1; i<sectionChain.size(); i++)
        for (int j=0; j<ini->getNumEntries(sectionChain[i]); j++)
            addEntry(convert(ini->getEntry(sectionChain[i], j)));
}

int SectionBasedConfiguration::getNumRunsInScenario(const char *scenarioName) const
{
    int sectionId = resolveConfigName(scenarioName);
    if (sectionId == -1)
        return 0;  // no such scenario or config
    else if (ini->getSectionName(sectionId)[0] != 'S')
        return 1;  // not "Scenario ...": config
    else
        return internalGetNumRunsInScenario(sectionId);
}

int SectionBasedConfiguration::internalGetNumRunsInScenario(int sectionId) const
{
    // extract all iteration specs from values within this section
    std::vector<IterationSpec> v = collectIterationSpecs(sectionId);
    validateIterations(v);

    // see if there's a condition given
    int conditionEntryId = internalFindEntry(sectionId, "condition");
    const char *condition = conditionEntryId!=-1 ? ini->getEntry(sectionId, conditionEntryId).getValue() : NULL;
    try {
        return Scenario(v, condition).getNumRuns();
    } catch (std::exception& e) {
        throw cRuntimeError("Scenario generator: %s", e.what());
    }
}

std::vector<std::string> SectionBasedConfiguration::unrollScenario(const char *scenarioName, bool detailed) const
{
    int sectionId = resolveConfigName(scenarioName);
    if (sectionId == -1)
        throw cRuntimeError("No such scenario: %s", scenarioName);

    // extract all iteration specs from values within this section
    std::vector<IterationSpec> iterspecs = collectIterationSpecs(sectionId);
    validateIterations(iterspecs);

    // see if there's a condition given
    int conditionEntryId = internalFindEntry(sectionId, "condition"); //XXX use constant (multiple places here!)
    const char *condition = conditionEntryId!=-1 ? ini->getEntry(sectionId, conditionEntryId).getValue() : NULL;

    // collect entryIds that we want to print out. Basically, only the entries
    // with ${...} in them -- which means the unique entryIds from the iterspecs.
    std::vector<int> entryIds;
    for (int i=0; i<iterspecs.size(); i++)
    {
        int entryId = iterspecs[i].entryId;
        if (std::find(entryIds.begin(), entryIds.end(), entryId)==entryIds.end())
            entryIds.push_back(entryId);
    }

    // iterate over all runs in the scenario
    try {
        Scenario scenario(iterspecs, condition);
        std::vector<std::string> result;
        if (scenario.restart())
        {
            for (;;)
            {
                // generate a string for the current run
                std::string runstring;
                if (!detailed)
                {
                    runstring = scenario.str();
                }
                else
                {
                    std::vector<std::string> values = scenario.get();
                    runstring += std::string("\t# ") + scenario.str() + "\n";
                    for (int i=0; i<entryIds.size(); i++)
                    {
                        int entryId = entryIds[i];
                        const cConfigurationReader::KeyValue& entry = ini->getEntry(sectionId, entryId);
                        std::string value = entry.getValue();
                        std::string actualValue = substitute(value, entryId, iterspecs, values);
                        runstring += std::string("\t") + entry.getKey() + " = " + actualValue + "\n";
                    }
                }
                result.push_back(runstring);

                // move to the next run
                if (!scenario.next())
                    break;
            }
        }
        return result;
    }
    catch (std::exception& e) {
        throw cRuntimeError("Scenario generator: %s", e.what());
    }
}

std::vector<SectionBasedConfiguration::IterationSpec> SectionBasedConfiguration::collectIterationSpecs(int sectionId) const
{
    std::vector<IterationSpec> v;
    for (int i=0; i<ini->getNumEntries(sectionId); i++)
    {
        const cConfigurationReader::KeyValue& entry = ini->getEntry(sectionId, i);
        const char *text = entry.getValue();
        const char *pos = text;
        while ((pos = strchr(pos, '$')) != NULL)
        {
            if (*(pos+1)=='{')
            {
                if (strcmp(entry.getKey(), "condition")==0)
                    throw cRuntimeError("Scenario generator: the ${...} syntax cannot be used within the condition= entry");

                const char *endPos = strchr(pos, '}');
                if (!endPos)
                    throw cRuntimeError("Scenario generator: missing '}' for '${' in entry %s = %s", entry.getKey(), entry.getValue());

                // parse what's inside the ${...}
                const char *varbegin = NULL;
                const char *varend = NULL;
                const char *valuebegin = NULL;

                const char *s = pos+2;
                while (isspace(*s)) s++;
                if (isalpha(*s))
                {
                    varbegin = varend = s;
                    while (isalnum(*varend)) varend++;
                    s = varend;
                    while (isspace(*s)) s++;
                    if (*s=='}') {
                        // ${x} syntax -- OK
                    }
                    else if (*s=='=' && *(s+1)!='=') {
                        // ${x=...} syntax -- OK
                        valuebegin = s+1;
                    }
                    else {
                        // missing equal sign: this is not a variable
                        valuebegin = varbegin;
                        varbegin = varend = NULL;
                    }
                } else {
                    valuebegin = s;
                }

                // fill in the struct
                IterationSpec loc;
                loc.entryId = i;
                loc.startPos = pos - text;
                loc.length = endPos - pos + 1;
                if (varbegin)
                    loc.varname.assign(varbegin, varend-varbegin);
                if (valuebegin)
                    loc.value.assign(valuebegin, endPos-valuebegin);
                v.push_back(loc);

                pos = endPos;
            }
            else
            {
                // nothing here, skip this '$' sign
                pos++;
            }
        }
    }
    return v;
}

void SectionBasedConfiguration::validateIterations(const std::vector<IterationSpec>& list) const
{
    // check that the same var is not defined twice, with different iteration specs
    std::set<std::string> varnames;
    for (int i=0; i<list.size(); i++)
    {
        const IterationSpec& loc = list[i];
        if (!loc.varname.empty() && !loc.value.empty())
        {
            if (varnames.find(loc.varname) != varnames.end())
                throw cRuntimeError("Scenario generator: iteration variable ${%s} defined multiple times in the configuration", loc.varname.c_str());
            varnames.insert(loc.varname);
        }
    }
}

std::string SectionBasedConfiguration::substitute(const std::string& value, int entryId, const std::vector<IterationSpec>& iterspecs, const std::vector<std::string>& values)
{
    // substitute iteration values
    std::string result = value;
    int stringOffset = 0;
    for (int i=0; i<iterspecs.size(); i++)
    {
        if (iterspecs[i].entryId==entryId) // IterationSpec refers to this key-value line
        {
            result.erase(iterspecs[i].startPos+stringOffset, iterspecs[i].length);
            result.insert(iterspecs[i].startPos+stringOffset, values[i]);
            stringOffset += values[i].length() - iterspecs[i].length;
        }
    }
    return result;
}

std::vector<int> SectionBasedConfiguration::resolveSectionChain(const char *sectionName) const
{
    // determine the list of sections, from this one following the "extends" chain up to [General]
    std::vector<int> sectionChain;
    int generalSectionId = internalFindSection("General");
    int sectionId = internalGetSectionId(sectionName);
    while (true)
    {
        if (std::find(sectionChain.begin(), sectionChain.end(), sectionId) != sectionChain.end())
            throw cRuntimeError("Circularity detected in section fallback chain at: [%s]", ini->getSectionName(sectionId));
        sectionChain.push_back(sectionId);
        int entryId = internalFindEntry(sectionId, "extends");
        std::string extends = entryId==-1 ? "" : ini->getEntry(sectionId, entryId).getValue();
        if (extends.empty() && generalSectionId!=-1 && sectionId!=generalSectionId)
            extends = "General";
        if (extends.empty())
            break;
        sectionId = resolveConfigName(extends.c_str());
        if (sectionId == -1)
            break; // wrong config name
    }

    return sectionChain;
}

void SectionBasedConfiguration::addEntry(const KeyValue1& entry)
{
    const std::string& key = entry.key;
    const char *lastDot = strrchr(key.c_str(), '.');
    if (!lastDot)
    {
        // config: add if not already in there
        if (PatternMatcher::containsWildcards(key.c_str()))
            throw cRuntimeError("invalid config key '%s': config keys cannot contain wildcard characters", key.c_str());
        if (config.find(key)==config.end())
            config[key] = entry;
    }
    else
    {
        // key contains a dot: parameter or per-object configuration
        // Note: since the last part of they key might contain widcards, it is not really possible
        // to distinguish the two. Cf "recording-interval", "recording-*" and "recording*"

        // analyze key and create appropriate entry
        std::string ownerName;
        std::string groupName;
        bool isApplyDefault;
        splitKey(key.c_str(), ownerName, groupName, isApplyDefault);
        bool groupContainsWildcards = PatternMatcher::containsWildcards(groupName.c_str());

        KeyValue2 entry2(entry);
        entry2.ownerPattern = new PatternMatcher(ownerName.c_str(), true, true, true);
        entry2.groupPattern = groupContainsWildcards ? new PatternMatcher(groupName.c_str(), true, true, true) : NULL;
        entry2.isApplyDefault = isApplyDefault;
        if (isApplyDefault)
            entry2.applyDefaultValue = strcmp(entry.value.c_str(), "true")==0;

        // find which group it should go into
        if (!groupContainsWildcards)
        {
            // no wildcard in group name
            if (groups.find(groupName)==groups.end()) {
                // group not yet exists, create it
                Group& group = groups[groupName];

                // initialize group with matching wildcard keys seen so far
                for (int k=0; k<wildcardGroup.entries.size(); k++)
                    if (wildcardGroup.entries[k].groupPattern->matches(groupName.c_str()))
                        group.entries.push_back(wildcardGroup.entries[k]);
            }
            groups[groupName].entries.push_back(entry2);
        }
        else
        {
            // groupName contains wildcards: we need to add it to all existing groups it matches
            wildcardGroup.entries.push_back(entry2);
            for (std::map<std::string,Group>::iterator it = groups.begin(); it!=groups.end(); it++)
                if (entry2.groupPattern->matches(it->first.c_str()))
                    (it->second).entries.push_back(entry2);
        }
    }
}

void SectionBasedConfiguration::splitKey(const char *key, std::string& outOwnerName, std::string& outGroupName, bool& outIsApplyDefault)
{
    std::string tmp = key;
    int keyLen = strlen(key);

    outIsApplyDefault = false;
    if (keyLen>14 && strcmp(key+keyLen-14, ".apply-default")==0)
    {
        // cut off ".apply-default"
        outIsApplyDefault = true;
        tmp = std::string(key, keyLen-14);
        key = tmp.c_str();
    }

    const char *lastDotPos = strrchr(key, '.');
    if (!lastDotPos) {
        // like "**.apply-default": group is "**", and owner is empty
        //FIXME DOES NOT WORK
        outOwnerName = "";
        outGroupName = key;
    }
    else {
        // normal case: group is the part after the last dot
        outOwnerName.assign(key, lastDotPos - key);
        outGroupName.assign(lastDotPos+1);
    }
}

SectionBasedConfiguration::KeyValue1 SectionBasedConfiguration::convert(const cConfigurationReader::KeyValue& e)
{
    StringSet::iterator it = basedirs.find(e.getBaseDirectory());
    if (it == basedirs.end()) {
        basedirs.insert(e.getBaseDirectory());
        it = basedirs.find(e.getBaseDirectory());
    }
    std::string *basedirRef = &(*it);
    return KeyValue1(basedirRef, e.getKey(), e.getValue());
}

int SectionBasedConfiguration::internalFindSection(const char *section) const
{
    // not very efficient (linear search), but we only invoke it a few times during activateConfig()
    for (int i=0; i<ini->getNumSections(); i++)
        if (strcmp(section, ini->getSectionName(i))==0)
            return i;
    return -1;
}

int SectionBasedConfiguration::internalGetSectionId(const char *section) const
{
    int sectionId = internalFindSection(section);
    if (sectionId == -1)
        throw cRuntimeError("no such section: %s", section);
    return sectionId;
}

int SectionBasedConfiguration::internalFindEntry(const char *section, const char *key) const
{
    return internalFindEntry(internalGetSectionId(section), key);
}

int SectionBasedConfiguration::internalFindEntry(int sectionId, const char *key) const
{
    // not very efficient (linear search), but we only invoke from activateConfig(),
    // and only once per section
    for (int i=0; i<ini->getNumEntries(sectionId); i++)
        if (strcmp(key, ini->getEntry(sectionId, i).getKey())==0)
            return i;
    return -1;
}

static int findInArray(const char *s, const char **array)
{
    for (int i=0; array[i]!=NULL; i++)
        if (!strcmp(s, array[i]))
            return i;
    return -1;
}

void SectionBasedConfiguration::validateConfig() const
{
    const char *obsoleteSections[] = {
        "Parameters", "Cmdenv", "Tkenv", "OutVectors", "Partitioning", "DisplayStrings", NULL
    };
    const char *cmdenvNames[] = {
        "autoflush", "event-banner-details", "event-banners", "express-mode",
        "message-trace", "module-messages", "output-file", "performance-display",
        "runs-to-execute", "status-frequency", NULL
    };
    const char *tkenvNames[] = {
        "anim-methodcalls", "animation-enabled", "animation-msgclassnames",
        "animation-msgcolors", "animation-msgnames", "animation-speed",
        "default-run", "expressmode-autoupdate", "image-path", "methodcalls-delay",
        "next-event-markers", "penguin-mode", "plugin-path", "print-banners",
        "senddirect-arrows", "show-bubbles", "show-layouting", "slowexec-delay",
        "update-freq-express", "update-freq-fast", "use-mainwindow",
        "use-new-layouter", NULL
    };

    // warn for obsolete section names and config keys
    for (int i=0; i<ini->getNumSections(); i++)
    {
        const char *section = ini->getSectionName(i);
        if (findInArray(section, obsoleteSections) != -1)
            throw cRuntimeError("Obsolete section name [%s] found, please convert the ini file to 4.x format", section);

        int numEntries = ini->getNumEntries(i);
        for (int j=0; j<numEntries; j++)
        {
            const char *key = ini->getEntry(i, j).getKey();
            if (findInArray(key, cmdenvNames) != -1 || findInArray(key, tkenvNames) != -1)
                throw cRuntimeError("Obsolete configuration key %s= found, please convert the ini file to 4.x format", key);
        }
    }

    // check section names; also make sure names of Configs and Scenarios don't clash
    std::set<std::string> configNames;
    for (int i=0; i<ini->getNumSections(); i++)
    {
        const char *section = ini->getSectionName(i);
        const char *configName = NULL;
        if (strcmp(section, "General")==0)
            ; // OK
        else if (strncmp(section, "Config ", 7)==0)
            configName  = section+7;
        else if (strncmp(section, "Scenario ", 9)==0)
            configName  = section+9;
        else
            throw cRuntimeError("Invalid section name [%s], should be [General], [Config <name>] or [Scenario <name>]", section);
        if (configName)
        {
            for (const char *s=configName; *s; s++)
                if (!isalnum(*s) && strchr("-_@", *s)==NULL)
                    throw cRuntimeError("Invalid section name [%s], contains illegal character '%c'", section, *s);
            if (configNames.find(configName)!=configNames.end())
                throw cRuntimeError("Configuration name '%s' not unique", configName, section);
            configNames.insert(configName);
        }

    }

    // check keys
    for (int i=0; i<ini->getNumSections(); i++)
    {
        const char *section = ini->getSectionName(i);
        int numEntries = ini->getNumEntries(i);
        for (int j=0; j<numEntries; j++)
        {
            const char *key = ini->getEntry(i, j).getKey();
            bool containsDot = strchr(key, '.')!=NULL;

            if (!containsDot)
            {
                // warn for unrecognized (or misplaced) config keys
                // NOTE: values don't need to be validated here, that will be
                // done when the config gets actually used
                cConfigEntry *e = (cConfigEntry *) configEntries.instance()->lookup(key);
                if (!e)
                    throw cRuntimeError("Unknown configuration key: %s", key);
                if (e->isPerObject())
                    throw cRuntimeError("Configuration key %s should be specified per object, try **.%s=", key, key);
                if (e->isGlobal() && strcmp(section, "General")!=0)
                    throw cRuntimeError("Configuration key %s may only occur in the [General] section", key);

                // check section hierarchy
                if (strcmp(key, "extends")==0)
                {
                    if (strcmp(section, "General")==0)
                        throw cRuntimeError("The [General] section cannot extend other sections");

                    // warn for invalid "extends" names
                    const char *value = ini->getEntry(i, j).getValue();
                    if (configNames.find(value)==configNames.end())
                        throw cRuntimeError("No such config or scenario: %s", value);
                    //FIXME warn for section circularity
                }
            }
            else
            {
                // check for per-object configuration subkeys (".enabled", ".interval", ".use-default")
                std::string ownerName;
                std::string groupName;
                bool isApplyDefault;
                splitKey(key, ownerName, groupName, isApplyDefault);
                //FIXME if groupName contains '-'...; must not contain wildcard etc
            }
        }
    }
}

const char *SectionBasedConfiguration::getConfigValue(const char *key) const
{
    std::map<std::string,KeyValue1>::const_iterator it = config.find(key);
    return it==config.end() ? NULL : it->second.value.c_str();
}

const cConfiguration::KeyValue& SectionBasedConfiguration::getConfigEntry(const char *key) const
{
    std::map<std::string,KeyValue1>::const_iterator it = config.find(key);
    return it==config.end() ? (KeyValue&)nullEntry : (KeyValue&)it->second;
}

std::vector<const char *> SectionBasedConfiguration::getMatchingConfigKeys(const char *pattern) const
{
    std::vector<const char *> result;
    PatternMatcher matcher(pattern, true, true, true);

    // iterate over the map -- this is going to be sloooow...
    for (std::map<std::string,KeyValue1>::const_iterator it = config.begin(); it != config.end(); ++it)
        if (matcher.matches(it->first.c_str()))
            result.push_back(it->first.c_str());
    return result;
}

const char *SectionBasedConfiguration::getParameterValue(const char *moduleFullPath, const char *paramName, bool hasDefaultValue) const
{
    const SectionBasedConfiguration::KeyValue2& entry = (KeyValue2&) getParameterEntry(moduleFullPath, paramName, hasDefaultValue);
    // NULL ==> not found,   "" ==> apply the default value
    return entry.getKey()==NULL ? NULL : entry.isApplyDefault ? "" : entry.value.c_str();
}

const cConfiguration::KeyValue& SectionBasedConfiguration::getParameterEntry(const char *moduleFullPath, const char *paramName, bool hasDefaultValue) const
{
    // look up which group; paramName serves as group name
    std::map<std::string,Group>::const_iterator it = groups.find(paramName);
    const Group *group = it==groups.end() ? &wildcardGroup : &it->second;

    // find first match in the group
    bool dontApplyDefault = false;
    for (int i=0; i<group->entries.size(); i++)
    {
        const KeyValue2& entry = group->entries[i];
        if (entry.ownerPattern->matches(moduleFullPath) && (entry.groupPattern==NULL || entry.groupPattern->matches(paramName)))
        {
            if (entry.isApplyDefault)
            {
                if (dontApplyDefault)
                    ; // ignore this apply-default line
                else if (entry.applyDefaultValue)
                    return entry;  // ==> "yes, apply the default value"
                else
                    dontApplyDefault = true; // ignore further .apply-default=true lines
            }
            else
                return entry;  // found value
        }
    }
    return nullEntry; // not found
}

const char *SectionBasedConfiguration::getPerObjectConfigValue(const char *objectFullPath, const char *keySuffix) const
{
    const SectionBasedConfiguration::KeyValue2& entry = (KeyValue2&) getPerObjectConfigEntry(objectFullPath, keySuffix);
    return entry.getKey()==NULL ? NULL : entry.value.c_str();
}

const cConfiguration::KeyValue& SectionBasedConfiguration::getPerObjectConfigEntry(const char *objectFullPath, const char *keySuffix) const
{
    // look up which group; keySuffix serves as group name
    std::map<std::string,Group>::const_iterator it = groups.find(keySuffix);
    const Group *group = it==groups.end() ? &wildcardGroup : &it->second;

    // find first match in the group
    for (int i=0; i<group->entries.size(); i++)
    {
        const KeyValue2& entry = group->entries[i];
        if (!entry.isApplyDefault && entry.ownerPattern->matches(objectFullPath) && (entry.groupPattern==NULL || entry.groupPattern->matches(keySuffix)))
            return entry;  // found value
    }
    return nullEntry; // not found
}

static const char *partAfterLastDot(const char *s)
{
    const char *lastDotPos = strrchr(s, '.');
    return lastDotPos==NULL ? NULL : lastDotPos+1;
}

std::vector<const char *> SectionBasedConfiguration::getMatchingPerObjectConfigKeys(const char *objectFullPath, const char *keySuffixPattern) const
{
    std::vector<const char *> result;

    // check all groups whose name matchs the pattern
    PatternMatcher matcher(keySuffixPattern, true, true, true); //XXX check flags
    for (std::map<std::string,Group>::const_iterator it = groups.begin(); it != groups.end(); ++it)
    {
        if (matcher.matches(it->first.c_str()))
        {
            // find all matching entries from this group.
            // We'll have a little problem where key ends in wildcard (i.e. entry.groupPattern!=NULL);
            // there we'd have to determine whether two *patterns* match. We resolve this
            // by checking whether one pattern matcher the other as string, and vica versa.
            const Group& group = it->second;
            for (int i=0; i<group.entries.size(); i++)
            {
                const KeyValue2& entry = group.entries[i];
                if (!entry.isApplyDefault && entry.ownerPattern->matches(objectFullPath) && (entry.groupPattern==NULL || matcher.matches(partAfterLastDot(entry.key.c_str())) || entry.groupPattern->matches(keySuffixPattern)))
                    result.push_back(entry.key.c_str());
            }
        }
    }
    return result;
}

void SectionBasedConfiguration::dump() const
{
    printf("Config:\n");
    for (std::map<std::string,KeyValue1>::const_iterator it = config.begin(); it!=config.end(); it++)
        printf("  %s = %s\n", it->first.c_str(), it->second.value.c_str());

    for (std::map<std::string,Group>::const_iterator it = groups.begin(); it!=groups.end(); it++)
    {
        const std::string& groupName = it->first;
        const Group& group = it->second;
        printf("Group %s:\n", groupName.c_str());
        for (int i=0; i<group.entries.size(); i++)
            printf("  %s = %s\n", group.entries[i].key.c_str(), group.entries[i].value.c_str());
    }
    printf("Wildcard Group:\n");
    for (int i=0; i<wildcardGroup.entries.size(); i++)
        printf("  %s = %s\n", wildcardGroup.entries[i].key.c_str(), wildcardGroup.entries[i].value.c_str());
}
