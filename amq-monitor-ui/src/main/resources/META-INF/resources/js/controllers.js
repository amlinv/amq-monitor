/*
 * Copyright 2015 AML Innovation & Consulting LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

/* Controllers */

var amqMonitorApp = angular.module('amqMonitorApp', ['ngAnimate', 'custom-filters', 'angular-loading-bar']);

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// VIEW SELECTOR
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
amqMonitorApp.controller('viewSelector', function($scope, $http) {
    $scope.view = 'monitor';

    $scope.showMonitor = function() {
      $scope.view = 'monitor';
    };
});

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// MONITOR CONTROLLER
//
//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
amqMonitorApp.controller('amqMonitorCtrl', function($scope, $http) {
    $scope.debug_log = "Start of debug log. ";
    $scope.connection_state = "monitor initializing";

    $scope.monitoredBrokers = [];
    $scope.displayQueueStats = [];

    $scope.show_producer_count = true;
    $scope.show_consumer_count = true;
    $scope.show_queue_size = true;
    $scope.show_enqueue = true;
    $scope.show_dequeue = true;
    $scope.show_cursor_pct_usage = true;
    $scope.show_mem_pct_usage = true;
    $scope.show_inflight_count = true;
    $scope.show_dequeue_rate = true;

    $scope.queueData = {
        queues: {}
    };


    try {
        var scheme;
        var host_and_port;
        var path;
        if (window.location.protocol == "https:") {
            scheme = "wss://";
        } else {
            scheme = "ws://";
        }
        host_and_port = window.location.host;
        path = window.location.pathname.replace(/[^/]*$/, "");

        var source = new WebSocket(scheme + host_and_port + path + "/ws/monitor");

        source.onopen = function (event) {
            $scope.$apply(function() { $scope.connection_state = "monitor connected" });
        };
        source.onmessage = function (event) {
            $scope.$apply(function() {
                var msg = JSON.parse(event.data);

                if ( msg.action == "brokerStats" ) {
                    $scope.onMonitorBrokerStats(msg.data);
                } else if ( msg.action == "queueStats" ) {
                    $scope.onMonitorQueueStats(msg.data);
                } else if ( msg.action == "queueAdded" ) {
                    $scope.onMonitoredQueueAdded(msg.data);
                } else if ( msg.action == "queueRemoved" ) {
                    $scope.onMonitoredQueueRemoved(msg.data);
                }

                //if ( $scope.debug_log ) {
                    $scope.debug_log = event.data;
                //}
            });
        };
        source.onerror = function (event) {
            $scope.$apply(
                function()
                {
                    $scope.note = "websocket error";
                }
            )
        };

        source.onclose = function (event) {
            $scope.$apply(
                function()
                {
                    $scope.connection_state = "monitor disconnected";
                }
            );
        }
    } catch ( exc ) {
        $scope.note = "websocket error";
    }

    $scope.onMonitorBrokerStats = function(stats) {
        if ( ( stats ) && ( stats.brokerStats ) && ( stats.brokerStats.brokerName ) ) {
            var brokerName = stats.brokerStats.brokerName;
            var index;
            if ( brokerName in $scope.monitoredBrokers ) {
                index = $scope.monitoredBrokers[brokerName];
            } else {
                index = $scope.monitoredBrokers.length;
                $scope.monitoredBrokers[brokerName] = index;
            }

            $scope.monitoredBrokers[index] = stats;

            if ( stats.queueStats ) {
                for (var queueName in stats.queueStats) {
                    if ( queueName in $scope.queueData.queues ) {
                        var allBrokerDetailsForQueue = $scope.queueData.queues[queueName].brokerDetails;
                        if ( ! ( brokerName in allBrokerDetailsForQueue ) ) {
                            allBrokerDetailsForQueue[brokerName] = { 'brokerName': brokerName };
                        }

                        var oneBrokerDetailsForQueue = allBrokerDetailsForQueue[brokerName];
                        var incomingQueueStatsForBroker = stats.queueStats[queueName];
                        for ( var attr in incomingQueueStatsForBroker ) {
                            oneBrokerDetailsForQueue[attr] = incomingQueueStatsForBroker[attr];
                        }
                    }
                }
            }
        }
    };

    $scope.onMonitorQueueStats = function(stats) {
        if ( stats ) {
            for ( var queueName in stats ) {
                var updatedStats = stats[queueName];

                if ( ! ( queueName in $scope.queueData.queues ) ) {
                    $scope.onMonitoredQueueAdded(queueName);
                }

                //
                // Copy over all of the statistics (copy individual values to keep the object reference unchanged).
                //  Statistics are copied instead of simply replacing the entire structure for performance; otherwise,
                //  angular appears to spend a lot more time, probably from making DOM modifications, causing notable
                //  delay for the user navigating the page and increase in CPU usage.
                //
                var queueData = $scope.queueData.queues[queueName];
                for ( var attr in updatedStats ) {
                    queueData[attr] = updatedStats[attr];
                }
            }
        }
    };

    $scope.addMonitorBroker = function() {
        var spec = prompt("Please specify the broker location and broker name in the format " +
                          "broker-name/location; broker-name is optional", "broker/location");

        if ( spec != "" ) {
            var brokerName = "*";
            var brokerLocation = "";

            var pos = spec.indexOf("/");
            if ( pos != -1 ) {
                brokerName = spec.substr(0, pos);
                brokerLocation = spec.substr(pos + 1);
            } else {
                brokerLocation = spec;
            }

            $scope.sendAddMonitorBroker(brokerName, brokerLocation);
        }
    };

    $scope.removeMonitorBroker = function() {
        var spec = prompt("Please specify the broker location and broker name in the format " +
                          "broker-name/location; broker-name is optional", "broker/location");

        if ( spec != "" ) {
            var brokerName = "*";
            var brokerLocation = "";

            var pos = spec.indexOf("/");
            if ( pos != -1 ) {
                brokerName = spec.substr(0, pos);
                brokerLocation = spec.substr(pos + 1);
            } else {
                brokerLocation = spec;
            }

            $scope.sendRemoveMonitorBroker(brokerName, brokerLocation);
        }
    };

    $scope.addMonitorQueue = function() {
        var queueName = prompt("Please specify the queue to monitor, or * to add all known queues");

        if ( queueName != "" ) {
            $scope.sendAddMonitorQueue(queueName);
        }
    };

    $scope.removeMonitorQueue = function() {
        var queueName = prompt("Please specify the queue to remove, or * to remove all known queues");

        if ( queueName != "" ) {
            $scope.sendRemoveMonitorQueue(queueName);
        }
    };

    $scope.startMonitor = function() {
        $scope.sendStartMonitor();
    };

    $scope.sendAddMonitorBroker = function(brokerName, brokerLocation) {
        var requestBody = {
            "brokerName": brokerName,
            "address" : brokerLocation
        };

        //$scope.debug("Send add-monitor-broker to server: " + JSON.stringify(requestBody));
        $http.put(
            'api/monitor/broker/',
            $.param(requestBody),
            { "headers" : { "Accept" : "application/json", 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then (
            function(response) {
                $scope.note = "added broker " + brokerName + "/" + brokerLocation;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error adding broker " + brokerName + "/" + brokerLocation;
            }
        );
    };

    $scope.sendRemoveMonitorBroker = function(brokerName, brokerLocation) {
        var requestBody = {
            "brokerName": brokerName,
            "address" : brokerLocation
        };

        $http.delete(
            'api/monitor/broker/',
            { params: requestBody }
        ).then (
            function(response) {
                $scope.note = "removed broker " + brokerName + "/" + brokerLocation;
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error on remove broker " + brokerName + "/" + brokerLocation;
            }
        );
    };

    $scope.sendAddMonitorQueue = function(queueName) {
        var requestBody = {
            "queueName": queueName
        };

        //$scope.debug("Send add-monitor-broker to server: " + JSON.stringify(requestBody));
        $http.put(
            'api/monitor/queue',
            $.param(requestBody),
            { "headers" : { 'Content-Type': 'application/x-www-form-urlencoded' } }
        ).then (
            function(response) {
                $scope.note = "added queue " + queueName;

                //
                // Process the list of queue names returned to make sure they are displayed in the UI.
                //
                var addedQueueNames = response.data;
                var iter = 0;
                while ( iter < addedQueueNames.length ) {
                    $scope.onMonitoredQueueAdded(addedQueueNames[iter]);
                    iter++;
                }
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error adding queue \"" + queueName;
            }
        );
    };

    $scope.sendRemoveMonitorQueue = function(queueName) {
        var requestBody = {
            "queueName": queueName
        };

        $http.delete(
            'api/monitor/queue',
            { params: requestBody }
        ).then (
            function(response) {
                $scope.note = "removed queue " + queueName;

                //
                // Remove the queues from the UI if they are still there.
                //
                var removedQueueNames = response.data;
                var iter = 0;
                while ( iter < removedQueueNames.length ) {
                    $scope.onMonitoredQueueRemoved(removedQueueNames[iter]);
                    iter++;
                }
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "error on remove queue " + queueName;
            }
        );
    };

    $scope.sendStartMonitor = function() {
        $http.get(
            'api/monitor/start'
        ).then (
            function(response) {
                $scope.note = "monitor started";
            }
            ,
            function(err) {
                /* Haven't found any useful error details in the argument(s) */
                $scope.note = "failed to start monitor";
            }
        );
    };

    $scope.onMonitoredQueueAdded = function (queueName) {
        //
        // Add the queue to the display, if it's not already added.
        //
        if ( ! ( queueName in $scope.queueData.queues ) ) {
            var stats = {
                brokerName: "totals",
                queueName: queueName,
                brokerDetails: {}
            };

            var newCount = $scope.displayQueueStats.push(stats);
            $scope.queueData.queues[queueName] = stats;
        }
    };

    $scope.onMonitoredQueueRemoved = function (queueName) {
        // Mark the entry as deleted; the user will see the indication and can trigger the actual removal.
        if ( queueName in $scope.queueData.queues ) {
            var queueStats = $scope.queueData.queues[queueName];
            queueStats.deleted = true;
            queueStats.dynamicClasses = "deletedItemPendingRemoval";
        }
    };

    /**
     * Check whether the named queue has been deleted and remove it from the UI now if so.
     *
     * @param queueName
     */
    $scope.checkRemoveQueueFromMonitorUi = function (queueName) {
        if ( queueName in $scope.queueData.queues ) {
            var rmStats = $scope.queueData.queues[queueName];

            if ( rmStats.deleted ) {
                rmStats.dynamicClasses = "";

                delete $scope.queueData.queues[queueName];

                var pos = $scope.displayQueueStats.indexOf(rmStats);
                if (pos != -1) {
                    $scope.displayQueueStats.splice(pos, 1);
                }
            }
        }
    };

    //
    // FILTERING
    //
    $scope.setupDestinationFilter = function (filterName) {
        if ( $scope.monitorDestinationQueryString ) {
            $scope.monitorDestinationQuery = eval("(" + $scope.monitorDestinationQueryString + ")");
        } else {
            $scope.monitorDestinationQuery = "";
        }
    };

    $scope.queueStatMatch = function(actual, expected) {
        var negate = false;
        var exact = false;

        //
        // Check for options.  First '!' negates, then '=' specifies exact match.  These must be in order (sorry).
        //
        if ( expected.charAt(0) == '!' ) {
            negate = true;
            expected = expected.substr(1);
        }
        if ( expected.charAt(0) == '=' ) {
            exact = true;
            expected = expected.substr(1);
        }

        var result;
        if ( exact ) {
            result = actual == expected;
        } else {
            result = ( actual.indexOf(expected) != -1 );
        }

        return result;
    };
});
