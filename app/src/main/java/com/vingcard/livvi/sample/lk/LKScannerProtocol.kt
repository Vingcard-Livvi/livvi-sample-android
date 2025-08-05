package com.vingcard.livvi.sample.lk

interface LKScannerProtocol
{
    fun didUpdateVisible(visibleDevices: List<LKScanResult>)
}