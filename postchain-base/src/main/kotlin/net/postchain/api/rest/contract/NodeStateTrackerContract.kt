package net.postchain.api.rest.contract

interface NodeStateTrackerContract

class BlockHeight(val blockHeight: Long)        : NodeStateTrackerContract
class MyStatus(val myStatus: String)            : NodeStateTrackerContract
class NodeStatuses(val statuses: Array<String>) : NodeStateTrackerContract
